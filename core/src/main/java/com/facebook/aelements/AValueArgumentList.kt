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
import com.intellij.psi.PsiExpressionList
import org.jetbrains.kotlin.psi.KtValueArgumentList

/**
 * Represents a list of value arguments (not to be confused with type arguements) in a call to a
 * function, i.e. `(a, b)` in `f(a, b)`
 *
 * Why does this exist and it's not just a list of the parameters? This since this list is a node by
 * itself, for example getting the parenthesis marking it is both useful and makes sense to
 * represent in an AST.
 */
open class AValueArgumentList internal constructor(psiElement: PsiElement) :
    AElementImpl(psiElement) {
  constructor(psiExpressionList: PsiExpressionList) : this(psiExpressionList as PsiElement)

  constructor(ktAnnotated: KtValueArgumentList) : this(ktAnnotated as PsiElement)

  override val javaElement: PsiExpressionList?
    get() = castJavaElement()

  override val kotlinElement: KtValueArgumentList?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiExpressionList, out KtValueArgumentList>
    get() = castIfLanguage()

  val valueArguments: List<AExpressionOrStatement>
    get() =
        javaElement?.expressions?.toList()?.map { it.toAElement() }
            ?: kotlinElement!!.arguments.map { it.getArgumentExpression()!!.toAElement() }

  val leftParenthesis: AElement
    get() = ifLanguage(isJava = { it.firstChild }, isKotlin = { it.leftParenthesis })!!.toAElement()

  val rightParenthesis: AElement
    get() = ifLanguage(isJava = { it.lastChild }, isKotlin = { it.rightParenthesis })!!.toAElement()
}
