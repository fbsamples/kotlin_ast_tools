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
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceParameterList
import org.jetbrains.kotlin.psi.KtTypeArgumentList

/**
 * Represents a list of type arguments (not to be confused with value arguements) in a call to a
 * function, i.e. `<A, B>` in `f<A, B>(a, b)`
 *
 * Why does this exist and it's not just a list of the types? This is because this list is a node by
 * itself, for example getting the braces marking it is both useful and makes sense to represent in
 * an AST.
 */
open class ATypeArgumentList internal constructor(psiElement: PsiElement) :
    AElementImpl(psiElement) {
  constructor(psiExpressionList: PsiReferenceParameterList) : this(psiExpressionList as PsiElement)

  constructor(ktAnnotated: KtTypeArgumentList) : this(ktAnnotated as PsiElement)

  override val javaElement: PsiReferenceParameterList?
    get() = castJavaElement()

  override val kotlinElement: KtTypeArgumentList?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiReferenceParameterList, out KtTypeArgumentList>
    get() = castIfLanguage()

  val typeArguments: List<ATypeReference>
    get() =
        javaElement?.typeParameterElements?.toList()?.map { it.toAElement() }
            ?: kotlinElement!!.arguments.map { it.typeReference!!.toAElement() }
}
