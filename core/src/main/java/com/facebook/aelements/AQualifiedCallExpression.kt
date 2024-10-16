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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

/**
 * Represents a function call which is qualified , i.e. `a.foo(1)`
 *
 * The hierarchy for qualified expressions (fields or method calls or references) and call
 * expressions are materially different between Java and Kotlin in their Psi repesentation, and this
 * creates a bunch of chaos which we try to bridge here.
 *
 * The expression `a.b.c(d)` in Java is represented as:
 * ```
 *           PsiMethodCallExpression `a.b.c(d)`
 *                                   /      \
 *                                  /        \
 *       PsiReferenceExpression `a.b.c`    PsiExpressionList `(d)`
 * ```
 *
 * That is, the top node splits into the full qualification and the parameters
 *
 * In Kotlin however, it is represented as:
 * ```
 *          KtQualifiedExpression `a.b.c(d)`
 *                                 /    \
 *                                /      \
 *       KtQualifiedExpression `a.b`     KtCallExpression `c(d)`
 * ```
 *
 * We want to allow handling both call expressions, and qualified expressions and make them easy to
 * find. So we break this to multiple types:
 * 1. ACallExpression - Represents a call expression, which is a method call or a constructor call
 * 2. AQualifiedExpression - Represents a qualified expression, which is a field access or a method
 *    call or a reference
 * 3. AQualifiedCallExpression - Represents a qualified expression which is a method call or a
 *    constructor call
 */
open class AQualifiedCallExpression private constructor(psiElement: PsiElement) :
    AExpressionImpl(psiElement), ACallExpression, AQualifiedExpression {
  constructor(
      psiMethodCallExpression: PsiMethodCallExpression
  ) : this(psiMethodCallExpression as PsiElement)

  constructor(
      ktQualifiedExpression: KtQualifiedExpression
  ) : this(ktQualifiedExpression as PsiElement)

  override val javaElement: PsiMethodCallExpression?
    get() = castJavaElement()

  override val kotlinElement: KtQualifiedExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiMethodCallExpression, out KtQualifiedExpression>
    get() = castIfLanguage()

  override val callExpressionKotlinElement: KtCallExpression?
    get() = kotlinElement?.selectorExpression as? KtCallExpression

  override val javaQualifiedExpression: PsiReferenceExpression?
    get() = javaElement?.methodExpression
}
