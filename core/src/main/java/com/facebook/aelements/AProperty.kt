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

import org.jetbrains.kotlin.com.intellij.lang.jvm.JvmModifier
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isPublic

/** Represents a decalration of a field or variable, either member or local */
open class AProperty internal constructor(psiElement: PsiElement) :
    AVariableDeclaration(psiElement) {
  constructor(psiVariable: PsiVariable) : this(psiVariable as PsiElement)

  constructor(ktProperty: KtProperty) : this(ktProperty as PsiElement)

  override val javaElement: PsiVariable?
    get() = castJavaElement()

  override val kotlinElement: KtProperty?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiVariable, out KtProperty>
    get() = castIfLanguage()

  val initializer: AExpressionOrStatement?
    get() = (javaElement?.initializer?.toAElement() ?: kotlinElement?.initializer?.toAElement())

  /** Only available in Kotlin, i.e. `foo` in `val bar by foo` */
  val delegateExpression: AExpressionOrStatement?
    get() = kotlinElement?.delegateExpression?.toAElement()

  val initializerOrDelegateExpression: AExpressionOrStatement?
    get() = initializer ?: delegateExpression

  val isLocal: Boolean
    get() = this is ALocalProperty

  val isPrivate: Boolean
    get() =
        ifLanguage(isJava = { it.hasModifier(JvmModifier.PRIVATE) }, isKotlin = { it.isPrivate() })

  val isProtected: Boolean
    get() =
        ifLanguage(
            isJava = { it.hasModifier(JvmModifier.PROTECTED) }, isKotlin = { it.isProtected() })

  val isPackage: Boolean
    get() = ifLanguage(isJava = { !isPublic && !isPrivate && !isProtected }, isKotlin = { false })

  val isInternal: Boolean
    get() = ifLanguage(isJava = { false }, isKotlin = { it.hasModifier(KtTokens.INTERNAL_KEYWORD) })

  val isPublic: Boolean
    get() =
        ifLanguage(
            isJava = {
              it.hasModifier(JvmModifier.PUBLIC) ||
                  it.getParentOfType<PsiClass>(strict = true)?.isInterface == true
            },
            isKotlin = { it.isPublic })
}
