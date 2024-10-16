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

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile

/** Represents an entire file (which is the root of the AST) */
open class AFile internal constructor(psiFile: PsiFile) : AElementImpl(psiFile) {
  constructor(psiJavaFile: PsiJavaFile) : this(psiJavaFile as PsiFile)

  constructor(ktFile: KtFile) : this(ktFile as PsiFile)

  override val psiElement: PsiFile
    get() = super.psiElement as PsiFile

  override val javaElement: PsiJavaFile?
    get() = castJavaElement()

  override val kotlinElement: KtFile?
    get() = castKotlinElement()

  val packageName: String?
    get() = javaElement?.packageName ?: kotlinElement?.packageFqName?.asString()
}
