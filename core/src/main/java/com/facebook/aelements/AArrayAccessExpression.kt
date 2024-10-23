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

package com.facebook.aelements

import com.intellij.psi.PsiArrayAccessExpression
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtArrayAccessExpression

/**
 * Represents an expression accessing a Java array or using the Kotlin get operator, i.e. `map["a"]`
 */
class AArrayAccessExpression
internal constructor(
    psiElement: PsiElement,
) : AExpressionImpl(psiElement) {
  constructor(
      psiArrayAccessExpression: PsiArrayAccessExpression
  ) : this(psiArrayAccessExpression as PsiElement)

  constructor(
      ktArrayAccessExpression: KtArrayAccessExpression
  ) : this(ktArrayAccessExpression as PsiElement)

  override val javaElement: PsiArrayAccessExpression?
    get() = castJavaElement()

  override val kotlinElement: KtArrayAccessExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiArrayAccessExpression, out KtArrayAccessExpression>
    get() = castIfLanguage()

  /**
   * An element representing all the value arguments and the parenthesis they're
   *
   * in i.e. `(a, b)` in `foo(a, b)`
   *
   * This can be null if only a lambda argument exists, for example in `foo { ... }`
   */
  val arrayExpression: AExpressionOrStatement?
    get() =
        javaElement?.arrayExpression?.toAElement() ?: kotlinElement!!.arrayExpression?.toAElement()

  /** All arguments passed to the call, i.e. `a` and the lambda in `foo(a) { ... }` */
  val indexExpressions: List<AExpressionOrStatement>
    get() =
        (javaElement?.indexExpression?.let { listOf(it.toAElement()) }
            ?: kotlinElement!!.indexExpressions.map { it.toAElement() })

  val leftBracket: PsiElement
    get() = javaElement?.children?.first { it.text == "[" } ?: kotlinElement?.leftBracket!!

  val rightBracket: PsiElement
    get() = javaElement?.children?.first { it.text == "]" } ?: kotlinElement?.rightBracket!!
}
