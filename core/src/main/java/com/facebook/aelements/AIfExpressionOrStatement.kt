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
import com.intellij.psi.PsiIfStatement
import org.jetbrains.kotlin.psi.KtIfExpression

/** Represents an if construct, which is an expression in Kotlin, but a statement in Java */
open class AIfExpressionOrStatement private constructor(psiElement: PsiElement) :
    AExpressionOrStatementImpl(psiElement) {
  constructor(psiIfStatement: PsiIfStatement) : this(psiIfStatement as PsiElement)

  constructor(ktIfExpression: KtIfExpression) : this(ktIfExpression as PsiElement)

  override val javaElement: PsiIfStatement?
    get() = castJavaElement()

  override val kotlinElement: KtIfExpression?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiIfStatement, out KtIfExpression>
    get() = castIfLanguage()

  val condition: AExpressionOrStatement
    get() =
        (javaElement?.condition ?: kotlinElement?.condition)?.toAElement() as AExpressionOrStatement

  val then: AExpressionOrStatement
    get() = (javaElement?.thenBranch ?: kotlinElement?.then)?.toAElement() as AExpressionOrStatement

  val `else`: AExpressionOrStatement?
    get() =
        (javaElement?.elseBranch ?: kotlinElement?.`else`)?.toAElement() as AExpressionOrStatement?
}
