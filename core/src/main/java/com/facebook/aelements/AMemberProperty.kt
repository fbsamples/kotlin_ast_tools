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
import com.intellij.psi.PsiField
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Represents a member variable declaration. Note that in Kotlin Psi this is the same element as a
 * local declaration, but we make them into two different AElements to be consistent with Java
 */
open class AMemberProperty internal constructor(psiElement: PsiElement) : AProperty(psiElement) {
  constructor(psiField: PsiField) : this(psiField as PsiElement)

  constructor(ktProperty: KtProperty) : this(ktProperty as PsiElement)

  override val javaElement: PsiField?
    get() = super.javaElement as PsiField?

  override val kotlinElement: KtProperty?
    get() = super.kotlinElement as KtProperty?

  override val ifLanguage: Cases<out PsiField, out KtProperty>
    get() = super.ifLanguage as Cases<out PsiField, out KtProperty>
}
