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
import org.jetbrains.kotlin.com.intellij.psi.PsiParameterList
import org.jetbrains.kotlin.psi.KtParameterList

/**
 * Represents a list of parameters in a method declaration, i.e. `int a, int b` in `void f(int a,
 * int b)`
 *
 * Why does this exist and it's not just a list of the parameters? This since this list is a node by
 * itself, for example getting the parenthesis marking it is both useful and makes sense to
 * represent in an AST.
 */
open class AParameterList internal constructor(psiElement: PsiElement) : AElementImpl(psiElement) {
  constructor(psiExpressionList: PsiParameterList) : this(psiExpressionList as PsiElement)

  constructor(ktAnnotated: KtParameterList) : this(ktAnnotated as PsiElement)

  override val javaElement: PsiParameterList?
    get() = castJavaElement()

  override val kotlinElement: KtParameterList?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiParameterList, out KtParameterList>
    get() = castIfLanguage()

  val parameters: List<AParameter>
    get() =
        javaElement?.parameters?.toList()?.map { it.toAElement() }
            ?: kotlinElement!!.parameters.map { it.toAElement() }

  val leftParenthesis: AElement
    get() = ifLanguage(isJava = { it.firstChild }, isKotlin = { it.leftParenthesis })!!.toAElement()

  val rightParenthesis: AElement
    get() = ifLanguage(isJava = { it.lastChild }, isKotlin = { it.rightParenthesis })!!.toAElement()
}
