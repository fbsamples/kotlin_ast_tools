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
import org.jetbrains.kotlin.com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtCallableDeclaration

/**
 * Represents a decalration of a field, variable, or parameter, examples:
 * ```
 * // in Java
 * int x;
 * void f(int x) {}
 *
 * // in Kotlin
 * val x: Int
 * fun f(x: Int) {}
 * ```
 */
open class AVariableDeclaration internal constructor(psiElement: PsiElement) :
    AAnnotated(psiElement), ANamedElement {
  constructor(psiVariable: PsiVariable) : this(psiVariable as PsiElement)

  constructor(ktProperty: KtCallableDeclaration) : this(ktProperty as PsiElement)

  override val javaElement: PsiVariable?
    get() = castJavaElement()

  override val kotlinElement: KtCallableDeclaration?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiVariable, out KtCallableDeclaration>
    get() = castIfLanguage()

  /** The type definition of the variable, i.e. `int` in `int x` or `Int` in `x: Int` */
  val typeReference: ATypeReference?
    get() = (javaElement?.typeElement?.toAElement() ?: kotlinElement?.typeReference?.toAElement())
}
