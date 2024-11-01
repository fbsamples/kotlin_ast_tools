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
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtCallableDeclaration

/** Represents a method in Java or a function in Kotlin */
abstract class ACallableDeclarationOrLambda internal constructor(psiElement: PsiElement) :
    AAnnotated(psiElement) {

  override val javaElement: PsiMethod?
    get() = castJavaElement()

  override val kotlinElement: KtCallableDeclaration?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiMethod, out KtCallableDeclaration>
    get() = castIfLanguage()

  abstract val parameterList: AParameterList

  abstract val valueParameters: List<AParameter>
}