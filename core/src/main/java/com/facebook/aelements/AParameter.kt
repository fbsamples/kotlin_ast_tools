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
import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.psi.KtParameter

/* Represents a value parameter (not to be confused with a type parameter) in a function declaration. */
open class AParameter internal constructor(psiElement: PsiElement) :
    AVariableDeclaration(psiElement) {
  constructor(psiTypeElement: PsiParameter) : this(psiTypeElement as PsiElement)

  constructor(ktTypeReference: KtParameter) : this(ktTypeReference as PsiElement)

  override val javaElement: PsiParameter?
    get() = super.javaElement as PsiParameter?

  override val kotlinElement: KtParameter?
    get() = super.kotlinElement as KtParameter?

  override val ifLanguage: Cases<out PsiParameter, out KtParameter>
    get() = super.ifLanguage as Cases<out PsiParameter, out KtParameter>
}
