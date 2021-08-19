/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.interpreter.state.Common
import org.jetbrains.kotlin.ir.interpreter.state.Complex
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.state.UnknownState
import org.jetbrains.kotlin.ir.interpreter.state.Wrapper
import org.jetbrains.kotlin.ir.interpreter.state.asBooleanOrNull
import org.jetbrains.kotlin.ir.interpreter.state.convertToStringIfNeeded
import org.jetbrains.kotlin.ir.interpreter.state.isUnit
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.SpecialNames

internal class Evaluator(val irBuiltIns: IrBuiltIns, val transformer: IrElementTransformerVoid) {
    internal val environment = IrInterpreterEnvironment(irBuiltIns)
    internal val callStack: CallStack
        get() = environment.callStack
    private val interpreter = IrInterpreter(environment, emptyMap())

    internal fun evaluate(irExpression: IrExpression, args: List<State> = emptyList(), interpretOnly: Boolean = true): Boolean {
        return callStack.safeExecute {
            interpreter.interpret(
                {
                    this.newSubFrame(irExpression)
                    this.pushInstruction(if (interpretOnly) SimpleInstruction(irExpression) else CompoundInstruction(irExpression))
                    args.forEach { this.pushState(it) }
                },
                { this.dropSubFrame() }
            )
        }
    }

    private fun checkForDefaultsAndEvaluate(expression: IrFunctionAccessExpression, args: List<State>): Boolean {
        return when {
            expression.hasDefaults() -> {
                var index = 0
                val allArgs = (0 until expression.valueArgumentsCount).map {
                    when {
                        expression.getValueArgument(it) == null -> environment.convertToState(null, irBuiltIns.anyNType)
                        else -> args[index++]
                    }
                }
                evaluate(environment.getOrCreateCallWithDefaults(expression), allArgs, interpretOnly = true)
            }
            else -> evaluate(expression, args, interpretOnly = true)
        }
    }

    internal fun withRollbackOnFailure(block: () -> Boolean) {
        // TODO
    }

    internal fun State.drop() {
        if (this !is Complex) return
        fields.keys.forEach { symbol -> this.setField(symbol, UnknownState) }
        outerClass = null
        superWrapperClass = null
        if (this is Wrapper) {
            setField(IrPropertySymbolImpl(), UnknownState) // fake property
        }
    }

    fun interpret(block: IrReturnableBlock): IrElement {
        callStack.newFrame(block)
        fallbackIrStatements(block)
        callStack.dropFrame()
        return block
    }

    private fun IrFunctionAccessExpression.getExpectedArgumentsCount(): Int {
        val dispatch = dispatchReceiver?.let { 1 } ?: 0
        val extension = extensionReceiver?.let { 1 } ?: 0
        val valueArguments = (0 until valueArgumentsCount).count { this.getValueArgument(it) != null }
        return dispatch + extension + valueArguments
    }

    fun evalIrReturnValue(expression: IrReturn): State? {
        expression.value = expression.value.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun fallbackIrReturn(expression: IrReturn, value: State?): IrReturn {
        value?.let { callStack.pushState(it) }
        return expression
    }

    fun evalIrCallDispatchReceiver(expression: IrFunctionAccessExpression): State? {
        expression.dispatchReceiver = expression.dispatchReceiver?.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun evalIrCallExtensionReceiver(expression: IrFunctionAccessExpression): State? {
        expression.extensionReceiver = expression.extensionReceiver?.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun evalIrCallArgs(expression: IrFunctionAccessExpression): List<State?> {
        (0 until expression.valueArgumentsCount).forEach {
            expression.putValueArgument(it, expression.getValueArgument(it)?.transform(transformer, null))
        }
        val args = mutableListOf<State?>()
        (0 until expression.valueArgumentsCount).forEach {
            args += callStack.tryToPopState()
        }
        return args.reversed()
    }

    fun fallbackIrCall(expression: IrCall, dispatchReceiver: State?, extensionReceiver: State?, args: List<State?>): IrCall {
        val owner = expression.symbol.owner
        val actualArgs = listOfNotNull(dispatchReceiver, extensionReceiver, *args.toTypedArray())
        if (actualArgs.size != expression.getExpectedArgumentsCount()) {
            actualArgs.forEach { it.drop() }
            return expression
        }

        if (owner.fqName.startsWith("kotlin.") || EvaluationMode.ONLY_BUILTINS.canEvaluateFunction(owner, expression) || EvaluationMode.WITH_ANNOTATIONS.canEvaluateFunction(owner, expression)) {
            if (!checkForDefaultsAndEvaluate(expression, actualArgs)) {
                actualArgs.forEach { it.drop() }
            }
            // TODO if result is Primitive -> return const
        }
        return expression
    }

    fun fallbackIrConstructorCall(expression: IrConstructorCall, dispatchReceiver: State?, args: List<State?>): IrConstructorCall {
        val actualArgs = listOf(dispatchReceiver, *args.toTypedArray()).filterNotNull()
        if (actualArgs.size != expression.getExpectedArgumentsCount()) {
            actualArgs.forEach { it.drop() }
            return expression
        }

        val owner = expression.symbol.owner
        if (EvaluationMode.ONLY_BUILTINS.canEvaluateFunction(owner) || EvaluationMode.WITH_ANNOTATIONS.canEvaluateFunction(owner) || Wrapper.mustBeHandledWithWrapper(owner.parentAsClass)) {
            if (!checkForDefaultsAndEvaluate(expression, actualArgs)) {
                actualArgs.forEach { it.drop() }
            }
        }
        return expression
    }

    fun fallbackIrBlock(expression: IrBlock): IrExpression {
        callStack.newSubFrame(expression)
        fallbackIrStatements(expression)
        callStack.dropSubFrame()
        if (expression.origin == IrStatementOrigin.FOR_LOOP && expression.statements.last() !is IrWhileLoop) {
            return IrBlockImpl(expression.startOffset, expression.endOffset, expression.type, origin = null, expression.statements)
        }
        return expression
    }

    private fun fallbackIrStatements(container: IrContainerExpression) {
        val newStatements = mutableListOf<IrStatement>()
        for (i in container.statements.indices) {
            val newStatement = container.statements[i].transform(transformer, null) as IrStatement
            if (newStatement is IrBreakContinue) break
            newStatements += newStatement
            if (i != container.statements.lastIndex && callStack.peekState().isUnit()) callStack.popState()
        }
        container.statements.clear()
        container.statements.addAll(newStatements)
    }

    fun evalIrBranchCondition(branch: IrBranch): State? {
        branch.condition = branch.condition.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun evalIrBranchResult(branch: IrBranch): State? {
        branch.result = branch.result.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun fallbackIrBranch(branch: IrBranch, condition: State?): IrElement {
        callStack.rollbackAllChanges {
            evalIrBranchResult(branch)
            true
        }
        return branch
    }

    fun fallbackIrWhen(expression: IrWhen, beginFromIndex: Int = 0, inclusive: Boolean = true): IrExpression {
        callStack.removeAllMutatedVariablesAndFields {
            for (i in (beginFromIndex until expression.branches.size)) {
                val condition = if (!inclusive && i == beginFromIndex) null else evalIrBranchCondition(expression.branches[i])
                fallbackIrBranch(expression.branches[i], condition)
            }
            // TODO that to do if object is passed to some none compile time function? 1. only scan it and delete mutated fields 2. remove entire symbol from stack
            true
        }
        return expression
    }

    fun evalIrSetValue(expression: IrSetValue): State? {
        expression.value = expression.value.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun fallbackIrSetValue(expression: IrSetValue, value: State?): IrExpression {
        if (value == null) {
            callStack.dropState(expression.symbol)
            return expression
        }
        evaluate(expression, listOf(value))
        return expression
    }

    fun evalIrGetValue(expression: IrGetValue): State? {
        // TODO evaluate can throw exception, how to avoid?
        evaluate(expression, interpretOnly = false)
        return callStack.tryToPopState()
    }

    fun fallbackIrGetValue(expression: IrGetValue, value: State?): IrExpression {
        value?.let { callStack.pushState(it) } ?: run {
            if (expression.symbol.owner.name == SpecialNames.THIS) {
                expression.type.getClass()?.let { callStack.pushState(Common(it)) }
            }
        }
        return expression
    }

    fun <T> evalIrConst(expression: IrConst<T>): State? {
        evaluate(expression)
        return callStack.tryToPopState()
    }

    fun <T> fallbackIrConst(expression: IrConst<T>, value: State?): IrExpression {
        value?.let { callStack.pushState(it) }
        return expression
    }

    fun evalIrVariable(declaration: IrVariable): State? {
        declaration.initializer = declaration.initializer?.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun fallbackIrVariable(declaration: IrVariable, value: State?): IrStatement {
        val state = if (declaration.initializer == null) null else value ?: UnknownState
        callStack.storeState(declaration.symbol, state)
        return declaration
    }

    fun fallbackIrGetField(expression: IrGetField): IrExpression {
        evaluate(expression)
        return expression
    }

    fun evalIrSetFieldValue(expression: IrSetField): State? {
        // TODO copy assert from unfoldSetField
        expression.value = expression.value.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun fallbackIrSetField(expression: IrSetField, value: State?): IrExpression {
        value?.let { evaluate(expression, listOf(it)) }
        return expression
    }

    fun evalIrVarargElements(expression: IrVararg): List<State?> {
        val args = mutableListOf<State?>()
        (0 until expression.elements.size).forEach {
            expression.putElement(it, expression.elements[it].transform(transformer, null) as IrVarargElement)
            args += callStack.tryToPopState()
        }
        return args
    }

    fun fallbackIrVararg(expression: IrVararg, args: List<State?>): IrExpression {
        val actualArgs = args.filterNotNull()
        if (actualArgs.size != expression.elements.size) return expression

        evaluate(expression, actualArgs, interpretOnly = true)
        return expression
    }

    fun evalIrWhileCondition(expression: IrWhileLoop): State? {
        expression.condition = expression.condition.transform(transformer, null)
        return callStack.tryToPopState()
    }

    private class Copier(symbolRemapper: SymbolRemapper, typeRemapper: TypeRemapper) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
        override fun visitBreak(jump: IrBreak): IrBreak {
            return IrBreakImpl(jump.startOffset, jump.endOffset, jump.type, jump.loop)
        }

        override fun visitContinue(jump: IrContinue): IrContinue {
            return IrContinueImpl(jump.startOffset, jump.endOffset, jump.type, jump.loop)
        }
    }

    fun fallbackIrWhileLoop(expression: IrWhileLoop): IrExpression {
        val newBlock = IrBlockImpl(0, 0, expression.body!!.type)
        var completed = false

        callStack.removeAllMutatedVariablesAndFields {
            while (true) {
                val condition = evalIrWhileCondition(expression)?.asBooleanOrNull()
                if (condition == null) {
                    expression.body?.transformChildren(transformer, null) // transform for 2 reasons: 1. remove mutated vars; 2. optimize that is possible
                    return@removeAllMutatedVariablesAndFields true
                }
                if (condition == false) {
                    completed = true
                    break
                }

                val newBody = expression.body!!.deepCopyWithSymbols(ParentFinder().apply { expression.accept(this, null) }.parent, ::Copier)
//                if (newBody is IrBlock) {
//                    newBlock.statements += newBody.statements[1]
//                } else {
//                    newBlock.statements += newBody
//                }
                newBlock.statements += newBody
                newBody.transformChildren(transformer, null)

                val jump = when {
                    newBody is IrBreakContinue -> {
                        newBlock.statements.removeLast()
                        newBody
                    }
                    newBody is IrContainerExpression && newBody.statements.last() is IrBreakContinue -> {
                        newBody.statements.removeLast() as IrBreakContinue
                    }
                    else -> null
                }
                if (jump is IrBreak) {
                    if (jump.loop == expression) { completed = true; break } else return jump
                } else if (jump is IrContinue) {
                    if (jump.loop == expression) continue else return jump
                }
            }
            false
        }
        return if (completed) newBlock else expression
    }

    fun evalIrTypeOperatorValue(expression: IrTypeOperatorCall): State? {
        expression.argument = expression.argument.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun fallbackIrTypeOperator(expression: IrTypeOperatorCall, arg: State?): IrExpression {
        if (arg != null) evaluate(expression, listOf(arg), interpretOnly = true)
        return expression
    }

    fun evalIrPropertyReferenceDispatchReceiver(expression: IrPropertyReference): State? {
        expression.dispatchReceiver = expression.dispatchReceiver?.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun evalIrPropertyReferenceExtensionReceiver(expression: IrPropertyReference): State? {
        expression.extensionReceiver = expression.extensionReceiver?.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun fallbackIrPropertyReference(expression: IrPropertyReference, dispatchReceiver: State?, extensionReceiver: State?): IrExpression {
        if (expression.dispatchReceiver != null && dispatchReceiver == null) return expression
        if (expression.extensionReceiver != null && extensionReceiver == null) return expression
        // TODO do we need check for compile time annotation here?
        evaluate(expression, listOfNotNull(dispatchReceiver, extensionReceiver), interpretOnly = true)
        return expression
    }

    fun fallbackIrStringConcatenation(expression: IrStringConcatenation, args: List<State?>): IrExpression {
        val actualArgs = args.filterNotNull()
        if (actualArgs.size != expression.arguments.size) return expression

        evaluate(expression, actualArgs, interpretOnly = true)
        return expression
    }

    fun evalIrStringConcatenationArgs(expression: IrStringConcatenation): List<State?> {
        expression as IrStringConcatenationImpl // TODO find better solution how to replace arguments in string concatenation
        val args = mutableListOf<State?>()
        (0 until expression.arguments.size).forEach { index ->
            expression.arguments[index] = expression.arguments[index].transform(transformer, null)
            val state = callStack.tryToPopState() ?: return@forEach
            args += state.convertToStringIfNeeded(environment) {
                val toStringCall = it.createToStringIrCall()
                val receiverParameter = toStringCall.symbol.owner.dispatchReceiverParameter!!
                toStringCall.dispatchReceiver = receiverParameter.createGetValue()

                callStack.newSubFrame(toStringCall.symbol.owner)
                callStack.storeState(receiverParameter.symbol, it)
                transformer.visitCall(toStringCall)
                callStack.dropSubFrame()
                callStack.tryToPopState()
            }
        }
        return args
    }

    fun evalIrGetClassArgument(expression: IrGetClass): State? {
        expression.argument = expression.argument.transform(transformer, null)
        return callStack.tryToPopState()
    }

    fun fallbackIrGetClass(expression: IrGetClass, argument: State?): IrExpression {
        if (argument == null) return expression
        evaluate(expression, listOf(argument), interpretOnly = true)
        return expression
    }

    fun fallbackIrClassReference(expression: IrClassReference): IrExpression {
        evaluate(expression, emptyList(), interpretOnly = true)
        return expression
    }
}

private class ParentFinder : IrElementVisitorVoid {
    var parent: IrDeclarationParent? = null
    override fun visitElement(element: IrElement) {
        if (element is IrDeclarationBase && parent == null) {
            parent = element.parent
            return
        }
        element.acceptChildren(this, null)
    }
}
