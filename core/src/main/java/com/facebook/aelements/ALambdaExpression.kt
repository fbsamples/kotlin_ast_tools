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
import org.jetbrains.kotlin.com.intellij.psi.PsiLambdaExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

class ALambdaExpression internal constructor(psiElement: PsiElement) :
    ADeclarationOrLambdaWithBody, AExpressionImpl(psiElement) {
  constructor(psiLambdaExpression: PsiLambdaExpression) : this(psiLambdaExpression as PsiElement)

  constructor(ktLambdaExpression: KtLambdaExpression) : this(ktLambdaExpression as PsiElement)

  override val javaElement: PsiLambdaExpression?
    get() = castJavaElement()

  override val kotlinElement: KtLambdaExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiLambdaExpression, out KtLambdaExpression>
    get() = castIfLanguage()

  override val bodyExpression: AElement?
    get() = javaElement?.body?.toAElement() ?: kotlinElement?.bodyExpression?.toAElement()
}
