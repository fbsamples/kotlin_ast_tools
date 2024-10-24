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

import org.jetbrains.kotlin.com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/** Represents an element with a name */
interface ANamedElement : AElement {

  override val javaElement: PsiNameIdentifierOwner?
    get() = castJavaElement()

  override val kotlinElement: KtNamedDeclaration?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiNameIdentifierOwner, out KtNamedDeclaration>
    get() = castIfLanguage()

  val name: String?
    get() = javaElement?.name ?: kotlinElement?.name
}
