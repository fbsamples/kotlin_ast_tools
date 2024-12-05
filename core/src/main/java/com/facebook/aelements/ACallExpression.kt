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
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Represents a call expression, i.e. `invoke(a)`
 *
 * See [AQualifiedCallExpression] for explanations of the mess involved in bridgeing this for Java
 * and Kotlin
 */
interface ACallExpression : AMethodOrNewCallExpression {

  override val javaElement: PsiMethodCallExpression?
    get() = castJavaElement()

  override val kotlinElement: KtExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiMethodCallExpression, out KtExpression>
    get() = castIfLanguage()

  /** The short name of the method being called, i.e. `foo` in `a.b.c.foo(d)` */
  val unqualifiedCalleeName: String?
    get() =
        javaElement?.methodExpression?.referenceName
            ?: callExpressionKotlinElement?.calleeExpression?.text
}

class ACallExpressionImpl
internal constructor(
    psiElement: PsiElement,
) : AMethodOrNewCallExpressionImpl(psiElement), ACallExpression {
  constructor(psiExpression: PsiReferenceExpression) : this(psiExpression as PsiElement)

  constructor(ktExpression: KtCallExpression) : this(ktExpression as PsiElement)
}
