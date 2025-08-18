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

import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.allConstructors

/** Represents a class or object, i.e. `public class Foo {}` or `object : Foo {}` */
open class AClassOrObject internal constructor(psiAElement: PsiElement) :
    AAnnotated(psiAElement), AModifierListOwner, ATypeParameterListOwner {
  constructor(psiClass: PsiClass) : this(psiClass as PsiElement)

  constructor(ktClassOrObject: KtClassOrObject) : this(ktClassOrObject as PsiElement)

  override val javaElement: PsiClass?
    get() = castJavaElement()

  override val kotlinElement: KtClassOrObject?
    get() = castKotlinElement()

  override val ifLanguage: Cases<out PsiClass, out KtClassOrObject>
    get() = castIfLanguage()

  val allConstructors: List<AConstructor>
    get() =
        javaElement?.constructors?.map { AConstructor(it) }
            ?: kotlinElement?.allConstructors?.map { AConstructor(it) }
            ?: emptyList()

  val methods: List<ANamedFunction>
    get() =
        javaElement
            ?.methods
            ?.filterNot { psiMethod -> psiMethod.isConstructor }
            ?.mapNotNull { it.toAElement() as ANamedFunction }
            ?: kotlinElement!!.declarations.filterIsInstance<KtNamedFunction>().map {
              it.toAElement()
            }

  val properties: List<AMemberProperty>
    get() =
        javaElement?.fields?.map { it.toAElement() }?.toList()
            ?: kotlinElement!!.declarations.filterIsInstance<KtProperty>().map {
              it.toAElement() as AMemberProperty
            }

  val name: String?
    get() = javaElement?.name ?: kotlinElement?.name

  val superTypes: List<ATypeReference>
    get() =
        ifLanguage(
            isKotlin = { kotlinElement ->
              kotlinElement.superTypeListEntries.mapNotNull { it.typeReference?.toAElement() }
            },
            isJava = { javaElement ->
              if (javaElement.isEnum || javaElement.isAnnotationType) {
                emptyList()
              } else {
                (javaElement.implementsListTypes + javaElement.extendsListTypes).mapNotNull {
                  it?.toAElement()
                }
              }
            },
        )
}
