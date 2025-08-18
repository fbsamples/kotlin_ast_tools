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
import org.jetbrains.kotlin.com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isPublic

/** Represents a decalration that can have modifiers such as `private`, `public` etc */
interface AModifierListOwner : AElement {

  override val javaElement: PsiModifierListOwner?
    get() = castJavaElement()

  override val kotlinElement: KtModifierListOwner?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiModifierListOwner, out KtModifierListOwner>
    get() = castIfLanguage()

  val isPrivate: Boolean
    get() =
        ifLanguage(isJava = { it.hasModifier(JvmModifier.PRIVATE) }, isKotlin = { it.isPrivate() })

  val isProtected: Boolean
    get() =
        ifLanguage(
            isJava = { it.hasModifier(JvmModifier.PROTECTED) },
            isKotlin = { it.isProtected() },
        )

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
            isKotlin = { it.isPublic },
        )

  val isStatic: Boolean
    get() = ifLanguage(isJava = { it.hasModifier(JvmModifier.STATIC) }, isKotlin = { false })
}
