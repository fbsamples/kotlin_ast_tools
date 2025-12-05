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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiThrowStatement
import org.jetbrains.kotlin.psi.KtThrowExpression

/**
 * Represents a throw statement/expression, which is an expression in Kotlin, but a statement in
 * Java
 *
 * Example:
 * ```
 * throw IllegalArgumentException("Invalid value")
 * ```
 */
open class AThrowExpression private constructor(psiElement: PsiElement) :
    AExpressionOrStatementImpl(psiElement) {
  constructor(psiThrowStatement: PsiThrowStatement) : this(psiThrowStatement as PsiElement)

  constructor(ktThrowExpression: KtThrowExpression) : this(ktThrowExpression as PsiElement)

  override val javaElement: PsiThrowStatement?
    get() = castJavaElement()

  override val kotlinElement: KtThrowExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiThrowStatement, out KtThrowExpression>
    get() = castIfLanguage()

  /**
   * The exception being thrown, i.e. `IllegalArgumentException("Invalid value")` in `throw
   * IllegalArgumentException("Invalid value")`
   */
  val exception: AExpressionOrStatement?
    get() =
        (javaElement?.exception ?: kotlinElement?.thrownExpression)?.toAElement()
            as AExpressionOrStatement?
}
