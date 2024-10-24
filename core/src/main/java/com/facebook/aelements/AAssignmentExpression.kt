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

import org.jetbrains.kotlin.com.intellij.psi.PsiAssignmentExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBinaryExpression

/** Represents an assignment expression, i.e. `a = 1` but not a declaration such as `val a = 1` */
open class AAssignmentExpression private constructor(psiElement: PsiElement) :
    AExpressionImpl(psiElement) {
  constructor(psiExpression: PsiAssignmentExpression) : this(psiExpression as PsiElement)

  constructor(ktExpression: KtBinaryExpression) : this(ktExpression as PsiElement)

  override val javaElement: PsiAssignmentExpression?
    get() = castJavaElement()

  override val kotlinElement: KtBinaryExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiAssignmentExpression, out KtBinaryExpression>
    get() = castIfLanguage()

  val left: AExpression
    get() = (javaElement?.lExpression ?: kotlinElement?.left)?.toAElement() as AExpression

  val right: AExpression
    get() = (javaElement?.rExpression ?: kotlinElement?.right)?.toAElement() as AExpression
}
