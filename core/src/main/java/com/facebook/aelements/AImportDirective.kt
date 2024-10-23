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
import org.jetbrains.kotlin.com.intellij.psi.PsiImportStatementBase
import org.jetbrains.kotlin.com.intellij.psi.PsiImportStaticStatement
import org.jetbrains.kotlin.psi.KtImportDirective

/** Represents an import, i.e. `import com.foo.Foo` or `import static com.foo.Foo.ba` */
class AImportDirective private constructor(psiElement: PsiElement) : AElementImpl(psiElement) {
  constructor(
      psiImportStatementBase: PsiImportStatementBase
  ) : this(psiImportStatementBase as PsiElement)

  constructor(ktImportDirective: KtImportDirective) : this(ktImportDirective as PsiElement)

  override val javaElement: PsiImportStatementBase?
    get() = castJavaElement()

  override val kotlinElement: KtImportDirective?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiImportStatementBase, out KtImportDirective>
    get() = castIfLanguage()

  val fullyQualifiedName: String?
    get() = (javaElement?.importReference?.qualifiedName ?: kotlinElement?.importPath?.pathStr)

  val isStatic: Boolean
    get() = javaElement is PsiImportStaticStatement

  val alias: String?
    get() = kotlinElement?.alias?.name
}
