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

package com.facebook.matching

import org.jetbrains.kotlin.com.intellij.psi.PsiElement

interface Resolver {
  /** Tries to return the fully qualified name of the type of this PsiElement */
  fun resolveToFullyQualifiedType(psiElement: PsiElement): String?

  /**
   * Tries to return the fully qualified name of the type of this PsiElement, along with every type
   * it extends. e.g. class A extends BClass implements CInterface. class BClass extends
   * DAbstractClass. Calling this on an instance of A would return
   * [com.fqn.A, com.fqn.BClass, com.fqn.CInterface, com.fqn.DAbstractClass]
   */
  fun resolveToFullyQualifiedTypeAndSupertypes(psiElement: PsiElement): List<String>?

  companion object {
    val DEFAULT =
        object : Resolver {
          override fun resolveToFullyQualifiedType(psiElement: PsiElement): String? {
            error("Template was not built with a resolver to allow resolving of symbols")
          }

          override fun resolveToFullyQualifiedTypeAndSupertypes(
              psiElement: PsiElement
          ): List<String>? {
            error("Template was not built with a resolver to allow resolving of symbols")
          }
        }
  }
}
