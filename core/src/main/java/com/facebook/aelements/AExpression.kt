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
import com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Represents any expression. Note that Java has the concept of statements as well, but not Kotlin
 */
interface AExpression : AExpressionOrStatement {
  override val javaElement: PsiExpression?
    get() = castJavaElement()

  override val kotlinElement: KtExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiExpression, out KtExpression>
    get() = castIfLanguage()
}

open class AExpressionImpl internal constructor(psiElement: PsiElement) :
    AExpression, AElementImpl(psiElement) {
  constructor(psiExpression: PsiExpression) : this(psiExpression as PsiElement)

  constructor(ktExpression: KtExpression) : this(ktExpression as PsiElement)
}
