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

import org.jetbrains.kotlin.com.intellij.psi.PsiForeachStatement

/**
 * Represents a foreach loop in Java (enhanced for loop). For example: `for (String s : list) { ...
 * }`
 */
interface AForeachStatement : AExpressionOrStatement {
  /** For example: `String s` in `for (String s : list)` */
  val iterationParameter: AElement?

  /** For example: `list` in `for (String s : list)` */
  val iteratedValue: AExpression?

  /** The body of the foreach loop. */
  val body: AExpressionOrStatement?
}

class AForeachStatementImpl(override val psiElement: PsiForeachStatement) :
    AForeachStatement, AElementImpl(psiElement) {

  override val javaElement: PsiForeachStatement
    get() = psiElement

  override val kotlinElement: Nothing?
    get() = null

  override val iterationParameter: AElement?
    get() = psiElement.iterationParameter?.toAElement()

  override val iteratedValue: AExpression?
    get() = psiElement.iteratedValue?.toAElement()

  override val body: AExpressionOrStatement?
    get() = psiElement.body?.toAElement()

  override val ifLanguage: Cases<out PsiForeachStatement, Nothing>
    get() = castIfLanguage()
}
