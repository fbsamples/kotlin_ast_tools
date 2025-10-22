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

import org.jetbrains.kotlin.com.intellij.psi.PsiAnnotation
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/** Represents an annotation entry, i.e. `@Foo` or `@Foo(1)` */
open class AAnnotation internal constructor(psiElement: PsiElement) : AElementImpl(psiElement) {
  constructor(psiMethod: PsiAnnotation) : this(psiMethod as PsiElement)

  constructor(ktAnnotationEntry: KtAnnotationEntry) : this(ktAnnotationEntry as PsiElement)

  override val javaElement: PsiAnnotation?
    get() = castJavaElement()

  override val kotlinElement: KtAnnotationEntry?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiAnnotation, out KtAnnotationEntry>
    get() = castIfLanguage()

  val shortName =
      javaElement?.nameReferenceElement?.referenceName ?: kotlinElement?.shortName?.identifier ?: ""

  val valueArguments: List<AExpressionOrStatement>
    get() =
        (javaElement?.parameterList?.attributes?.mapNotNull {
          it.value?.toAElement() as? AExpressionOrStatement
        }
            ?: kotlinElement?.valueArgumentList?.arguments?.map {
              it.getArgumentExpression()!!.toAElement() as AExpressionOrStatement
            }) ?: emptyList()
}
