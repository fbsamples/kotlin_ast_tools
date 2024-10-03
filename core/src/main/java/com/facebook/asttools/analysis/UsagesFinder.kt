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

package com.facebook.asttools.analysis

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType

/**
 * Utility that finds reference and usages of varibles and functions in the same file
 *
 * For example, you could use this to find all AST elements that are referring to a variable
 */
object UsagesFinder {

  fun getUsages(
      declaration: KtNamedDeclaration,
      under: PsiElement = declaration.getTopmostParentOfType<KtFile>()!!,
  ): List<PsiElement> {
    val name = declaration.name ?: error("Declaration has no name")
    return under.collectDescendantsOfType<KtExpression> {
      it != declaration &&
          it.text == name &&
          DeclarationsFinder.getDeclarationAt(it, name) == declaration
    }
  }
}
