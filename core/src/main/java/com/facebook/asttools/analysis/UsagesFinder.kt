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

import com.facebook.aelements.AElement
import com.facebook.aelements.AFile
import com.facebook.aelements.ANamedElement
import com.facebook.aelements.getParentOfType
import com.facebook.aelements.toAElement
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPostfixExpression
import com.intellij.psi.PsiPrefixExpression
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType

/**
 * Utility that finds reference and usages of varibles and functions in the same file
 *
 * For example, you could use this to find all AST elements that are referring to a variable
 */
object UsagesFinder {

  /**
   * Find all elements that are references to the given declaration
   *
   * @param the declaration to search for its usages
   * @param under limit the results to elements under this element. If omitted, we search the entire
   *   file.
   */
  fun getUsages(
      declaration: KtNamedDeclaration,
      under: PsiElement = declaration.getTopmostParentOfType<KtFile>()!!,
  ): List<PsiElement> {
    return getUsages<KtExpression>(declaration, declaration.name, under)
  }

  /** See [getUsages] */
  fun getUsages(
      declaration: PsiVariable,
      under: PsiElement = declaration.getTopmostParentOfType<PsiJavaFile>()!!,
  ): List<PsiElement> {
    return getUsages<PsiExpression>(declaration, declaration.name, under)
  }

  /** See [getUsages] */
  fun getUsages(
      declaration: ANamedElement,
      under: AElement = declaration.getParentOfType<AFile>()!!,
  ): List<AElement> =
      when (val psiElement = declaration.psiElement) {
        is KtNamedDeclaration -> getUsages(psiElement, under.psiElement)
        is PsiVariable -> getUsages(psiElement, under.psiElement)
        else -> error("Unsupported declaration type ${declaration::class}")
      }.map { it.toAElement() }

  /**
   * Find all writes to the given variable
   *
   * We consider writes to be any expression that assigns a value to the variable (including its
   * initializer) or a modifying operator such as ++
   */
  fun getWrites(
      declaration: PsiVariable,
      under: PsiElement = declaration.getTopmostParentOfType<PsiJavaFile>()!!
  ): List<PsiElement> {
    return listOfNotNull(declaration.takeIf { it.initializer != null }) +
        getUsages(declaration, under).mapNotNull {
          it.parent.takeIf { parent ->
            parent is PsiAssignmentExpression && parent.lExpression == it ||
                parent is PsiPostfixExpression ||
                parent is PsiPrefixExpression
          }
        }
  }

  /** See [getWrites] */
  fun getWrites(
      declaration: KtNamedDeclaration,
      under: PsiElement = declaration.getTopmostParentOfType<KtFile>()!!
  ): List<PsiElement> {
    return listOfNotNull(
        declaration.takeIf {
          declaration is KtProperty &&
              (declaration.initializer != null || declaration.delegateExpression != null)
        }) +
        getUsages(declaration, under).mapNotNull {
          it.parent.takeIf { parent ->
            parent is KtBinaryExpression &&
                parent.operationReference.text == "=" &&
                parent.left == it ||
                parent is KtUnaryExpression && parent.operationReference.text in setOf("++", "--")
          }
        }
  }

  /** See [getWrites] */
  fun getWrites(
      declaration: ANamedElement,
      under: AElement = declaration.getParentOfType<AFile>()!!,
  ): List<AElement> =
      when (val psiElement = declaration.psiElement) {
        is KtNamedDeclaration -> getWrites(psiElement, under.psiElement)
        is PsiVariable -> getWrites(psiElement, under.psiElement)
        else -> error("Unsupported declaration type ${declaration::class}")
      }.map { it.toAElement() }

  /** Returns all access to the element which are not a modification */
  fun getReads(
      declaration: ANamedElement,
      under: AElement = declaration.getParentOfType<AFile>()!!,
  ): List<AElement> {
    val writes = getWrites(declaration, under)
    return getUsages(declaration, under).filter { it.parent !in writes }
  }

  private inline fun <reified T : PsiElement> getUsages(
      declaration: PsiElement,
      name: String?,
      under: PsiElement,
  ): List<PsiElement> {
    name ?: error("Declaration has no name")
    return under.collectDescendantsOfType<T> {
      it != declaration &&
          it.text == name &&
          DeclarationsFinder.getDeclarationAt(it, name) == declaration
    }
  }
}
