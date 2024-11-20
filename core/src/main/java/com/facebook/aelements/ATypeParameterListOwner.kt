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

import org.jetbrains.kotlin.com.intellij.psi.PsiTypeParameterListOwner
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner

/**
 * Represents a list of type parameter (not to be confused with the list of type arguments) in a
 * function, a class or an object definition i.e. `<A, B>` in `fun <A, B> f() {}` or `<A, B>` in
 * `class C<A, B> {}`
 */
interface ATypeParameterListOwner : AElement {

  override val javaElement: PsiTypeParameterListOwner?
    get() = castJavaElement()

  override val kotlinElement: KtTypeParameterListOwner?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiTypeParameterListOwner, out KtTypeParameterListOwner>
    get() = castIfLanguage()

  val typeParameters: List<ATypeParameter>
    get() =
        javaElement?.typeParameters?.toList()?.map { it.toAElement() }
            ?: kotlinElement!!.typeParameters.map { it.toAElement() }

  val typeConstraints: List<ATypeConstraint>
    get() = kotlinElement?.typeConstraints?.map { it.toAElement() } ?: emptyList()
}
