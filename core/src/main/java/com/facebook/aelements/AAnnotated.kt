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
import org.jetbrains.kotlin.com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.psi.KtAnnotated

/** Represents any AElement that can be annotated. */
open class AAnnotated internal constructor(psiElement: PsiElement) : AElementImpl(psiElement) {
  constructor(psiJvmModifierOwner: PsiModifierListOwner) : this(psiJvmModifierOwner as PsiElement)

  constructor(ktAnnotated: KtAnnotated) : this(ktAnnotated as PsiElement)

  override val javaElement: PsiModifierListOwner?
    get() = castJavaElement()

  override val kotlinElement: KtAnnotated?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiModifierListOwner, out KtAnnotated>
    get() = castIfLanguage()

  val annotations: List<AAnnotation>
    get() =
        javaElement?.annotations?.map { AAnnotation(it) }
            ?: kotlinElement!!.annotationEntries.map { AAnnotation(it) }
}
