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

package com.facebook.matching

import com.google.errorprone.annotations.CheckReturnValue
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class MatchResult<Element : PsiElement?>(
    val psiElement: Element,
    internal val matchedVariables: Map<String, PsiElement>,
) {

  fun getVariableResult(variableName: String): String? = matchedVariables[variableName]?.text

  fun hasValue(variableName: String): Boolean = matchedVariables.containsKey(variableName)

  operator fun get(variableName: String): String? = getVariableResult(variableName)
}

/**
 * Kotlin nodes matcher
 *
 * In order to avoid the need to create this class per every type it is built in a generic way
 * storing a list of children matchers.
 *
 * Create one of these matchers using [match]
 */
interface PsiAstMatcher<Element : PsiElement> {

  /**
   * Whether this matcher will match a null ot not, this is useful for building optional matchers
   */
  val shouldMatchToNull: Boolean

  @CheckReturnValue
  fun findAll(element: PsiElement): List<Element> {
    return findAllWithVariables(element).map { it.psiElement as Element }
  }

  @CheckReturnValue
  fun findAllWithVariables(element: PsiElement): List<MatchResult<Element>> {
    val results = mutableListOf<MatchResult<Element>>()
    element.accept(
        object : KtTreeVisitorVoid() {
          override fun visitElement(element: PsiElement) {
            val result = matches(element)
            if (result != null) {
              results.add(result as MatchResult<Element>)
            }
            super.visitElement(element)
          }
        }
    )
    return results
  }

  /**
   * Checks if the given element satisfies the conditions of this matcher
   * - If it is, a [MatchResult] is returned, which points to `obj` and contains all references to
   *   satisfy variables that are part of this matcher.
   * - If the element does not match a `null` is returned
   */
  fun matches(obj: PsiElement?): MatchResult<Element?>?
}
