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

import org.jetbrains.kotlin.com.intellij.psi.PsiCodeBlock
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression

/**
 * Represents a code block that is not a statement or expression, this can include a Java class body
 * for example
 */
interface ACodeBlock : AElement {

  override val javaElement: PsiCodeBlock?
    get() = castJavaElement()

  override val kotlinElement: KtBlockExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiCodeBlock, out KtBlockExpression>
    get() = castIfLanguage()

  val statements: List<AExpressionOrStatement>
    get() =
        (javaElement?.statements?.map { it.toAElement() }
            ?: kotlinElement!!.statements.map { it.toAElement() })

  val lBrace: PsiElement?
    get() = javaElement?.lBrace ?: kotlinElement!!.lBrace

  val rBrace: PsiElement?
    get() = javaElement?.rBrace ?: kotlinElement!!.rBrace
}

class ACodeBlockImpl private constructor(psiElement: PsiElement) :
    AElementImpl(psiElement), ACodeBlock {
  constructor(psiCodeBlock: PsiCodeBlock) : this(psiCodeBlock as PsiElement)

  constructor(ktBlockExpression: KtBlockExpression) : this(ktBlockExpression as PsiElement)
}
