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
import org.jetbrains.kotlin.com.intellij.psi.PsiTryStatement
import org.jetbrains.kotlin.psi.KtTryExpression

/**
 * Represents a try-catch-finally construct, which is an expression in Kotlin, but a statement in
 * Java
 *
 * Example:
 * ```
 * try {
 *   riskyOperation()
 * } catch (e: Exception) {
 *   handleError(e)
 * } finally {
 *   cleanup()
 * }
 * ```
 */
open class ATryExpression private constructor(psiElement: PsiElement) :
    AExpressionOrStatementImpl(psiElement) {
  constructor(psiTryStatement: PsiTryStatement) : this(psiTryStatement as PsiElement)

  constructor(ktTryExpression: KtTryExpression) : this(ktTryExpression as PsiElement)

  override val javaElement: PsiTryStatement?
    get() = castJavaElement()

  override val kotlinElement: KtTryExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiTryStatement, out KtTryExpression>
    get() = castIfLanguage()

  /** The try block, i.e. `{ riskyOperation() }` in `try { riskyOperation() } catch (e) { ... }` */
  val tryBlock: AElement?
    get() = (javaElement?.tryBlock ?: kotlinElement?.tryBlock)?.toAElement()

  /**
   * The catch clauses, i.e. `catch (e: Exception) { handleError(e) }` in a try-catch block. Returns
   * a list of catch blocks/clauses.
   */
  val catchClauses: List<AElement>
    get() =
        javaElement?.catchBlocks?.map { it.toAElement() }
            ?: kotlinElement?.catchClauses?.map { it.toAElement() }
            ?: emptyList()

  /** The finally block, i.e. `{ cleanup() }` in `try { ... } finally { cleanup() }`, if present */
  val finallyBlock: AElement?
    get() =
        (javaElement?.finallyBlock ?: kotlinElement?.finallyBlock?.finalExpression)?.toAElement()
}
