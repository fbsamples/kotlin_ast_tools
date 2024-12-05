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

import org.jetbrains.kotlin.com.intellij.psi.PsiCallExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Represents a call expression or a Java constructor call, i.e. `invoke(a)` ot `new Foo()`
 *
 * See [AQualifiedCallExpression] for explanations of the mess involved in bridgeing this for Java
 * and Kotlin
 */
interface AMethodOrNewCallExpression : AExpression {

  override val javaElement: PsiCallExpression?
    get() = castJavaElement()

  override val kotlinElement: KtExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiCallExpression, out KtExpression>
    get() = castIfLanguage()

  val callExpressionKotlinElement: KtCallExpression?

  /**
   * An element representing all the value arguments and the parenthesis they're
   *
   * in i.e. `(a, b)` in `foo(a, b)`
   *
   * This can be null if only a lambda argument exists, for example in `foo { ... }`
   */
  val valueArgumentList: AValueArgumentList?
    get() =
        javaElement?.argumentList?.toAElement()
            ?: callExpressionKotlinElement?.valueArgumentList?.toAElement()

  /** All arguments passed to the call, i.e. `a` and the lambda in `foo(a) { ... }` */
  val valueArguments: List<AExpressionOrStatement>
    get() =
        (javaElement?.argumentList?.expressions?.toList()
                ?: callExpressionKotlinElement?.valueArguments?.map {
                  it.getArgumentExpression()!!
                })!!
            .map { it.toAElement() as AExpressionOrStatement }

  /** The node of the list of all type arguments i.e. `<A>` in foo<A>() */
  val typeArgumentList: ATypeArgumentList?
    get() =
        javaElement?.typeArgumentList?.toAElement()
            ?: callExpressionKotlinElement?.typeArgumentList?.toAElement()

  /** All type arguments passed to the call, i.e. `A` in foo<A>() */
  val typeArguments: List<ATypeReference>
    get() =
        (javaElement?.typeArgumentList?.typeParameterElements?.toList()?.map { it.toAElement() }
            ?: callExpressionKotlinElement?.typeArguments?.map {
              it.typeReference!!.toAElement()
            })!!
}

open class AMethodOrNewCallExpressionImpl
internal constructor(
    psiElement: PsiElement,
) : AExpressionImpl(psiElement), AMethodOrNewCallExpression {
  constructor(psiExpression: PsiCallExpression) : this(psiExpression as PsiElement)

  constructor(ktExpression: KtCallExpression) : this(ktExpression as PsiElement)

  override val callExpressionKotlinElement: KtCallExpression?
    get() = kotlinElement as? KtCallExpression
}
