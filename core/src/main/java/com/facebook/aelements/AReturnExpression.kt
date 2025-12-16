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
import org.jetbrains.kotlin.com.intellij.psi.PsiReturnStatement
import org.jetbrains.kotlin.psi.KtReturnExpression

/**
 * Represents a return statement/expression, which is an expression in Kotlin, but a statement in
 * Java
 *
 * Example:
 * ```
 * return 42
 * ```
 */
open class AReturnExpression private constructor(psiElement: PsiElement) :
    AExpressionOrStatementImpl(psiElement) {
  constructor(psiReturnStatement: PsiReturnStatement) : this(psiReturnStatement as PsiElement)

  constructor(ktReturnExpression: KtReturnExpression) : this(ktReturnExpression as PsiElement)

  override val javaElement: PsiReturnStatement?
    get() = castJavaElement()

  override val kotlinElement: KtReturnExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiReturnStatement, out KtReturnExpression>
    get() = castIfLanguage()

  /**
   * The value being returned, i.e. `42` in `return 42`, or null for a bare `return` with no value
   */
  val returnValue: AExpressionOrStatement?
    get() =
        (javaElement?.returnValue ?: kotlinElement?.returnedExpression)?.toAElement()
            as AExpressionOrStatement?
}
