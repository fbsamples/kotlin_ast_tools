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
import org.jetbrains.kotlin.psi.KtForExpression

/**
 * Represents a for loop in Kotlin. For example: `for (item in collection) { ... }` or `for (i in
 * 0..9) { ... }`
 */
interface AForExpression : AExpression {
  /** For example: `item` in `for (item in collection)` */
  val loopParameter: AElement?

  /** For example: `collection` in `for (item in collection)` or `0..9` in `for (i in 0..9)` */
  val loopRange: AExpression?

  /** The body of the for loop. */
  val body: AExpressionOrStatement?
}

class AForExpressionImpl(override val psiElement: KtForExpression) :
    AForExpression, AElementImpl(psiElement) {

  override val javaElement: Nothing?
    get() = null

  override val kotlinElement: KtExpression
    get() = psiElement

  override val loopParameter: AElement?
    get() = psiElement.loopParameter?.toAElement()

  override val loopRange: AExpression?
    get() = psiElement.loopRange?.toAElement() as? AExpression

  override val body: AExpressionOrStatement?
    get() = psiElement.body?.toAElement()

  override val ifLanguage: Cases<Nothing, out KtExpression>
    get() = castIfLanguage()
}
