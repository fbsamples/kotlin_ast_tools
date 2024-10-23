/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.asttools

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Loads a single Psi element that matches text or name or both, this is meant to be used to keep
 * tests shorter and simpler
 */
inline fun <reified T : PsiElement> PsiElement.requireSingleOfType(
    text: String? = null,
    name: String? = null
): T {
  val results =
      collectDescendantsOfType<T> {
        (text == null || it.text == text) &&
            (name == null ||
                (it as? KtNamedDeclaration)?.name == name ||
                (it as? PsiNamedElement)?.name == name)
      }
  return results.singleOrNull()
      ?: run {
        val matchString =
            listOfNotNull(
                    text?.let { "text=\"$text\"" },
                    "type=${T::class.java.simpleName}"
                        .takeIf { T::class.java != PsiElement::class.java },
                    name?.let { "name=\"$name\"" })
                .joinToString(separator = ", and ")
        val resultsString =
            if (results.isEmpty()) "no elements"
            else
                "${results.size} elements:\n" +
                    results.withIndex().joinToString(separator = "\n") { (index, value) ->
                      "${index + 1}) " + value.text
                    }
        error(
            "Expected exactly one element to match $matchString under element, but found $resultsString")
      }
}

/**
 * Loads a single Psi element that matches text or name or both, this is meant to be used to keep
 * tests shorter and simpler
 */
fun PsiElement.requireSingle(
    text: String? = null,
    name: String? = null,
): PsiElement {
  return requireSingleOfType<PsiElement>(text, name)
}
