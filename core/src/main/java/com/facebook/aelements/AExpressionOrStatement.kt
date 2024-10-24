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
import org.jetbrains.kotlin.com.intellij.psi.PsiStatement
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Represents any expression or statement. Note that Java has the concept of statements as well, but
 * not Kotlin. For Kotlin this is equivalent to [AExpression]
 */
interface AExpressionOrStatement : AElement {
  override val javaElement: PsiElement?

  override val kotlinElement: KtExpression?

  override val ifLanguage: Cases<out PsiElement, out KtExpression>
}

open class AExpressionOrStatementImpl internal constructor(psiElement: PsiElement) :
    AExpressionOrStatement, AElementImpl(psiElement) {
  constructor(psiExpression: PsiExpression) : this(psiExpression as PsiElement)

  constructor(psiStatment: PsiStatement) : this(psiStatment as PsiElement)

  constructor(ktExpression: KtExpression) : this(ktExpression as PsiElement)

  override val javaElement: PsiElement?
    get() = castJavaElement()

  override val kotlinElement: KtExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiElement, out KtExpression>
    get() = castIfLanguage()
}
