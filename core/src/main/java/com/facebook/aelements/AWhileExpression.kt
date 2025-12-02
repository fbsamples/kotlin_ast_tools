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

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtWhileExpression

/** Represents a while loop in Kotlin. For example: `while (i < 10) { ... }` */
interface AWhileExpression : AExpression {
  /** For example: `i < 10` in `while (i < 10)` */
  val condition: AExpression?

  /** The body of the while loop. */
  val body: AExpressionOrStatement?
}

class AWhileExpressionImpl(override val psiElement: KtWhileExpression) :
    AWhileExpression, AElementImpl(psiElement) {

  override val javaElement: Nothing?
    get() = null

  override val kotlinElement: KtExpression
    get() = psiElement

  override val condition: AExpression?
    get() = psiElement.condition?.toAElement() as? AExpression

  override val body: AExpressionOrStatement?
    get() = psiElement.body?.toAElement()

  override val ifLanguage: Cases<Nothing, out KtExpression>
    get() = castIfLanguage()
}
