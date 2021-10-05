/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.test.components;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link GenerateNewCompilerTests.kt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/analysis-api/testData/components/expectedExpressionType")
@TestDataPath("$PROJECT_ROOT")
public class KtFe10ExpectedExpressionTypeTestGenerated extends AbstractKtFe10ExpectedExpressionTypeTest {
    @Test
    public void testAllFilesPresentInExpectedExpressionType() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/expectedExpressionType"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("functionExpressionBody.kt")
    public void testFunctionExpressionBody() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionExpressionBody.kt");
    }

    @Test
    @TestMetadata("functionExpressionBodyBlockExpression.kt")
    public void testFunctionExpressionBodyBlockExpression() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionExpressionBodyBlockExpression.kt");
    }

    @Test
    @TestMetadata("functionExpressionBodyQualified.kt")
    public void testFunctionExpressionBodyQualified() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionExpressionBodyQualified.kt");
    }

    @Test
    @TestMetadata("functionExpressionBodyWithTypeFromRHS.kt")
    public void testFunctionExpressionBodyWithTypeFromRHS() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionExpressionBodyWithTypeFromRHS.kt");
    }

    @Test
    @TestMetadata("functionExpressionBodyWithoutExplicitType.kt")
    public void testFunctionExpressionBodyWithoutExplicitType() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionExpressionBodyWithoutExplicitType.kt");
    }

    @Test
    @TestMetadata("functionLambdaParam.kt")
    public void testFunctionLambdaParam() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionLambdaParam.kt");
    }

    @Test
    @TestMetadata("functionNamedlParam.kt")
    public void testFunctionNamedlParam() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionNamedlParam.kt");
    }

    @Test
    @TestMetadata("functionParamWithTypeParam.kt")
    public void testFunctionParamWithTypeParam() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionParamWithTypeParam.kt");
    }

    @Test
    @TestMetadata("functionPositionalParam.kt")
    public void testFunctionPositionalParam() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionPositionalParam.kt");
    }

    @Test
    @TestMetadata("functionPositionalParamQualified.kt")
    public void testFunctionPositionalParamQualified() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/functionPositionalParamQualified.kt");
    }

    @Test
    @TestMetadata("ifCondition.kt")
    public void testIfCondition() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/ifCondition.kt");
    }

    @Test
    @TestMetadata("ifConditionQualified.kt")
    public void testIfConditionQualified() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/ifConditionQualified.kt");
    }

    @Test
    @TestMetadata("infixFunctionAsRegularCallParam.kt")
    public void testInfixFunctionAsRegularCallParam() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/infixFunctionAsRegularCallParam.kt");
    }

    @Test
    @TestMetadata("infixFunctionParam.kt")
    public void testInfixFunctionParam() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/infixFunctionParam.kt");
    }

    @Test
    @TestMetadata("infixFunctionParamQualified.kt")
    public void testInfixFunctionParamQualified() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/infixFunctionParamQualified.kt");
    }

    @Test
    @TestMetadata("lambdaWithExplicitTypeFromVariable.kt")
    public void testLambdaWithExplicitTypeFromVariable() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/lambdaWithExplicitTypeFromVariable.kt");
    }

    @Test
    @TestMetadata("lambdaWithoutReturnNorExplicitType.kt")
    public void testLambdaWithoutReturnNorExplicitType() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/lambdaWithoutReturnNorExplicitType.kt");
    }

    @Test
    @TestMetadata("propertyDeclaration.kt")
    public void testPropertyDeclaration() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/propertyDeclaration.kt");
    }

    @Test
    @TestMetadata("propertyDeclarationQualified.kt")
    public void testPropertyDeclarationQualified() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/propertyDeclarationQualified.kt");
    }

    @Test
    @TestMetadata("propertyDeclarationWithSafeCast.kt")
    public void testPropertyDeclarationWithSafeCast() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/propertyDeclarationWithSafeCast.kt");
    }

    @Test
    @TestMetadata("propertyDeclarationWithTypeCast.kt")
    public void testPropertyDeclarationWithTypeCast() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/propertyDeclarationWithTypeCast.kt");
    }

    @Test
    @TestMetadata("propertyDeclarationWithTypeFromRHS.kt")
    public void testPropertyDeclarationWithTypeFromRHS() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/propertyDeclarationWithTypeFromRHS.kt");
    }

    @Test
    @TestMetadata("propertyDeclarationWithoutExplicitType.kt")
    public void testPropertyDeclarationWithoutExplicitType() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/propertyDeclarationWithoutExplicitType.kt");
    }

    @Test
    @TestMetadata("returnFromFunction.kt")
    public void testReturnFromFunction() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/returnFromFunction.kt");
    }

    @Test
    @TestMetadata("returnFromFunctionQualifiedReceiver.kt")
    public void testReturnFromFunctionQualifiedReceiver() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/returnFromFunctionQualifiedReceiver.kt");
    }

    @Test
    @TestMetadata("returnFromFunctionQualifiedSelector.kt")
    public void testReturnFromFunctionQualifiedSelector() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/returnFromFunctionQualifiedSelector.kt");
    }

    @Test
    @TestMetadata("returnFromLambda.kt")
    public void testReturnFromLambda() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/returnFromLambda.kt");
    }

    @Test
    @TestMetadata("sam.kt")
    public void testSam() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/sam.kt");
    }

    @Test
    @TestMetadata("samAsArgument.kt")
    public void testSamAsArgument() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/samAsArgument.kt");
    }

    @Test
    @TestMetadata("samAsConstructorArgument.kt")
    public void testSamAsConstructorArgument() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/samAsConstructorArgument.kt");
    }

    @Test
    @TestMetadata("samAsReturn.kt")
    public void testSamAsReturn() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/samAsReturn.kt");
    }

    @Test
    @TestMetadata("samWithExplicitTypeFromProperty.kt")
    public void testSamWithExplicitTypeFromProperty() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/samWithExplicitTypeFromProperty.kt");
    }

    @Test
    @TestMetadata("samWithTypeCast.kt")
    public void testSamWithTypeCast() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/samWithTypeCast.kt");
    }

    @Test
    @TestMetadata("variableAssignment.kt")
    public void testVariableAssignment() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/variableAssignment.kt");
    }

    @Test
    @TestMetadata("variableAssignmentQualified.kt")
    public void testVariableAssignmentQualified() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/variableAssignmentQualified.kt");
    }

    @Test
    @TestMetadata("whileCondition.kt")
    public void testWhileCondition() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/whileCondition.kt");
    }

    @Test
    @TestMetadata("whileConditionQualified.kt")
    public void testWhileConditionQualified() throws Exception {
        runTest("analysis/analysis-api/testData/components/expectedExpressionType/whileConditionQualified.kt");
    }
}
