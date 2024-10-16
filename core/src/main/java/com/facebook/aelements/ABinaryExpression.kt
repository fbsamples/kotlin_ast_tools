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

import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBinaryExpression

/** Represents a binary expression, i.e. `a + 1` or `b && true` */
open class ABinaryExpression private constructor(psiElement: PsiElement) :
    AExpressionImpl(psiElement) {
  constructor(psiExpression: PsiBinaryExpression) : this(psiExpression as PsiElement)

  constructor(ktExpression: KtBinaryExpression) : this(ktExpression as PsiElement)

  override val javaElement: PsiBinaryExpression?
    get() = castJavaElement()

  override val kotlinElement: KtBinaryExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiBinaryExpression, out KtBinaryExpression>
    get() = castIfLanguage()

  val left: AExpression
    get() = (javaElement?.lOperand ?: kotlinElement?.left)?.toAElement() as AExpression

  val right: AExpression
    get() = (javaElement?.rOperand ?: kotlinElement?.right)?.toAElement() as AExpression

  val operator: String
    get() = (javaElement?.operationSign?.text ?: kotlinElement?.operationReference?.text)!!
}
