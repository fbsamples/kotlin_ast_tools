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

import org.jetbrains.kotlin.com.intellij.psi.PsiBreakStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBreakExpression

/**
 * Represents a break statement/expression, which is an expression in Kotlin, but a statement in
 * Java
 *
 * Example:
 * ```
 * while (condition) {
 *   if (shouldExit) break
 *   process()
 * }
 * ```
 *
 * Example with label:
 * ```
 * outer@ while (i < 10) {
 *   while (j < 10) {
 *     if (shouldExitOuter) break@outer
 *   }
 * }
 * ```
 */
open class ABreakExpression private constructor(psiElement: PsiElement) :
    AExpressionOrStatementImpl(psiElement) {
  constructor(psiBreakStatement: PsiBreakStatement) : this(psiBreakStatement as PsiElement)

  constructor(ktBreakExpression: KtBreakExpression) : this(ktBreakExpression as PsiElement)

  override val javaElement: PsiBreakStatement?
    get() = castJavaElement()

  override val kotlinElement: KtBreakExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiBreakStatement, out KtBreakExpression>
    get() = castIfLanguage()

  /**
   * The label identifier for labeled break statements, i.e. `outer` in `break@outer`, or null for
   * unlabeled breaks
   */
  val labelName: String?
    get() = javaElement?.labelIdentifier?.text ?: kotlinElement?.getLabelName()
}
