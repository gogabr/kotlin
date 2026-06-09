/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.backend.common.serialization.toStableJsIdentifier
import java.io.File


class MinimizedNameGenerator {
    private var index = 0
    private val functionSignatureToName = hashMapOf<String, String>()
    private val reservedNames = hashSetOf<String>()
    private val keptNames = hashSetOf<String>()

    fun generateNextName(seed: String): String {
        val result = seed.toStableJsIdentifier(reservedNames)
        return result
    }

    fun nameBySignature(signature: String): String {
        if (signature in keptNames) return signature
        return functionSignatureToName.getOrPut(signature) {
            generateNextName(signature)
        }
    }

    fun keepName(signature: String): Boolean {
        return keptNames.add(signature)
    }

    fun reserveName(signature: String) {
        reservedNames.add(signature)
    }

    fun clear() {
        index = 0
        functionSignatureToName.clear()
        reservedNames.clear()
    }
}
