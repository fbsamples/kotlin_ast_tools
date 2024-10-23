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
import org.jetbrains.kotlin.com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference

/** Represents the name of a type, e.g. `String` in `String s` or `Foo in fun f(foo: Foo)` */
open class ATypeReference internal constructor(psiElement: PsiElement) : AElementImpl(psiElement) {
  constructor(psiTypeElement: PsiTypeElement) : this(psiTypeElement as PsiElement)

  constructor(ktTypeReference: KtTypeReference) : this(ktTypeReference as PsiElement)

  override val javaElement: PsiTypeElement?
    get() = castJavaElement()

  override val kotlinElement: KtTypeReference?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiTypeElement, out KtTypeReference>
    get() = castIfLanguage()
}
