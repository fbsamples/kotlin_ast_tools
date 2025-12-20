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
import org.jetbrains.kotlin.com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

/**
 * Represents a qualified expression, a member or field access usually, i.e. `a.b`
 *
 * If the selector (i.e. b in the example above) is a function call, it will be represented as a
 * [AQualifiedCallExpression]
 *
 * See [AQualifiedCallExpression] for more explanations of the mess involved in bridgeing this for
 * Java and Kotlin
 */
interface AQualifiedExpression : AExpression {
  override val javaElement: PsiExpression?
    get() = castJavaElement()

  override val kotlinElement: KtQualifiedExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiExpression, out KtQualifiedExpression>
    get() = castIfLanguage()

  val javaQualifiedExpression: PsiReferenceExpression?

  val receiverExpressionOrStatement: AExpressionOrStatement
    get() =
        (javaQualifiedExpression?.qualifierExpression ?: kotlinElement?.receiverExpression)!!
            .toAElement() as AExpressionOrStatement

  val selectorExpression: AExpression
    get() =
        (javaQualifiedExpression?.element ?: kotlinElement?.selectorExpression)!!.toAElement()
            as AExpression

  val operator: String
    get() = if (language == Language.JAVA) "." else kotlinElement?.operationSign?.value!!
}

class AQualifiedExpressionImpl
internal constructor(
    psiElement: PsiElement,
) : AExpressionImpl(psiElement), AQualifiedExpression {
  constructor(psiExpression: PsiReferenceExpression) : this(psiExpression as PsiElement)

  constructor(ktExpression: KtQualifiedExpression) : this(ktExpression as PsiElement)

  override val javaQualifiedExpression: PsiReferenceExpression?
    get() = javaElement as PsiReferenceExpression?
}
