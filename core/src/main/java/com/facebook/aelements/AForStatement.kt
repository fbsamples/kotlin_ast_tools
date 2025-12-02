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

import org.jetbrains.kotlin.com.intellij.psi.PsiForStatement

/**
 * Represents a traditional for loop in Java. For example: `for (int i = 0; i < 10; i++) { ... }`
 */
interface AForStatement : AExpressionOrStatement {
  /** For example: `int i = 0` in `for (int i = 0; i < 10; i++)` */
  val initialization: AExpressionOrStatement?

  /** For example: `i < 10` in `for (int i = 0; i < 10; i++)` */
  val condition: AExpression?

  /** For example: `i++` in `for (int i = 0; i < 10; i++)` */
  val update: AExpressionOrStatement?

  /** The body of the for loop. */
  val body: AExpressionOrStatement?
}

class AForStatementImpl(override val psiElement: PsiForStatement) :
    AForStatement, AElementImpl(psiElement) {

  override val javaElement: PsiForStatement
    get() = psiElement

  override val kotlinElement: Nothing?
    get() = null

  override val initialization: AExpressionOrStatement?
    get() = psiElement.initialization?.takeUnless { it.text == ";" }?.toAElement()

  override val condition: AExpression?
    get() = psiElement.condition?.toAElement()

  override val update: AExpressionOrStatement?
    get() = psiElement.update?.takeUnless { it.text == ";" }?.toAElement()

  override val body: AExpressionOrStatement?
    get() = psiElement.body?.toAElement()

  override val ifLanguage: Cases<out PsiForStatement, Nothing>
    get() = castIfLanguage()
}
