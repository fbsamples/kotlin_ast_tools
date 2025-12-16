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

import org.jetbrains.kotlin.com.intellij.psi.PsiContinueStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtContinueExpression

/**
 * Represents a continue statement/expression, which is an expression in Kotlin, but a statement in
 * Java
 *
 * Example:
 * ```
 * while (condition) {
 *   if (shouldSkip) continue
 *   process()
 * }
 * ```
 *
 * Example with label:
 * ```
 * outer@ while (i < 10) {
 *   while (j < 10) {
 *     if (shouldSkipOuter) continue@outer
 *   }
 * }
 * ```
 */
open class AContinueExpression private constructor(psiElement: PsiElement) :
    AExpressionOrStatementImpl(psiElement) {
  constructor(psiContinueStatement: PsiContinueStatement) : this(psiContinueStatement as PsiElement)

  constructor(ktContinueExpression: KtContinueExpression) : this(ktContinueExpression as PsiElement)

  override val javaElement: PsiContinueStatement?
    get() = castJavaElement()

  override val kotlinElement: KtContinueExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiContinueStatement, out KtContinueExpression>
    get() = castIfLanguage()

  /**
   * The label identifier for labeled continue statements, i.e. `outer` in `continue@outer`, or null
   * for unlabeled continues
   */
  val labelName: String?
    get() = javaElement?.labelIdentifier?.text ?: kotlinElement?.getLabelName()
}
