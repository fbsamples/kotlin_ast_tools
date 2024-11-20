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
import org.jetbrains.kotlin.com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameter

/**
 * Represents a type parameter (not to be confused with a type argument) in function, class or
 * object definition i.e. `A` and `B` in `fun <A, B> f() {}` or `A` and `B` in `class C<A, B> {}`
 */
open class ATypeParameter internal constructor(psiElement: PsiElement) : AElementImpl(psiElement) {
  constructor(psiTypeParameter: PsiTypeParameter) : this(psiTypeParameter as PsiElement)

  constructor(ktTypeParameter: KtTypeParameter) : this(ktTypeParameter as PsiElement)

  override val javaElement: PsiTypeParameter?
    get() = castJavaElement()

  override val kotlinElement: KtTypeParameter?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiTypeParameter, out KtTypeParameter>
    get() = castIfLanguage()
}
