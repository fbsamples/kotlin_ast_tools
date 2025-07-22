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
import com.facebook.asttools.analysis.DeclarationsFinder.getDeclarationsAt
import org.jetbrains.kotlin.com.intellij.psi.PsiAssignmentExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodReferenceExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiPostfixExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiPrefixExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiThisExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
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
      declaration: PsiMethod,
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
    return under.collectDescendantsOfType<T> { candidate ->
      return@collectDescendantsOfType isUsage(candidate, declaration, name)
    }
  }

  private fun isUsage(candidate: PsiElement, declaration: PsiElement, name: String?): Boolean {
    if (candidate == declaration) {
      return false
    }
    val isPossibleUsage =
        when (candidate) {
          is KtQualifiedExpression ->
              candidate.receiverExpression is KtThisExpression &&
                  candidate.selectorExpression?.text == name
          is PsiMethodReferenceExpression -> candidate.text == "this::$name"
          is PsiReferenceExpression ->
              candidate.text == name ||
                  candidate.qualifier is PsiThisExpression &&
                      candidate.referenceNameElement?.text == name
          else -> candidate.text == name
        }
    if (!isPossibleUsage) {
      return false
    }

    val parent = candidate.parent
    val declarationsForName = getDeclarationsAt(candidate)[name] ?: return false
    val effectiveDeclaration =
        declarationsForName.allValues.firstOrNull { declarationForName ->
          // only match function to function, and variable to variable
          if (isFunction(declaration) != isFunction(declarationForName)) {
            return@firstOrNull false
          }

          when (parent) {
            is KtQualifiedExpression -> {
              // skip bar.foo when looking for foo usages
              if (parent.selectorExpression == candidate) {
                return@firstOrNull false
              }
            }
            is PsiReferenceExpression -> {
              if (candidate !is PsiMethodReferenceExpression) {
                // skip if parent is this.foo to avoid duplications
                if (parent.qualifier is PsiThisExpression) {
                  return@firstOrNull false
                }
                // skip bar.foo when looking for foo usages
                if (parent.qualifier != null && parent.referenceNameElement == candidate) {
                  return@firstOrNull false
                }
              }
            }
          }

          when (candidate) {
            is KtQualifiedExpression -> {
              val receiverExpression = candidate.receiverExpression
              if (receiverExpression is KtThisExpression) {
                val label = receiverExpression.getTargetLabel()
                if (label != null &&
                    declarationForName.getParentOfType<KtClassOrObject>(true)?.name !=
                        label.text.removePrefix("@")) {
                  false
                } else {
                  (declarationForName is KtProperty && !declarationForName.isLocal) ||
                      (declarationForName is KtParameter &&
                          declarationForName.valOrVarKeyword != null)
                }
              } else {
                false
              }
            }
            is PsiMethodReferenceExpression -> true
            is PsiReferenceExpression -> {
              val qualifier = candidate.qualifier
              if (qualifier is PsiThisExpression) {
                val label = qualifier.qualifier
                if (label != null &&
                    declarationForName.getParentOfType<PsiClass>(true)?.name != label.text) {
                  false
                } else {
                  declarationForName is PsiField
                }
              } else {
                true
              }
            }
            else -> true
          }
        }

    return effectiveDeclaration == declaration &&
        when {
          candidate is PsiMethodReferenceExpression -> effectiveDeclaration is PsiMethod
          parent is KtCallExpression ->
              if (parent.calleeExpression == candidate)
                  effectiveDeclaration is KtNamedFunction ||
                      (effectiveDeclaration as? KtProperty)
                          ?.typeReference
                          ?.text
                          .orEmpty()
                          .contains("->")
              else true
          parent is KtCallableReferenceExpression -> effectiveDeclaration is KtNamedFunction
          parent is PsiMethodCallExpression ->
              parent.methodExpression == candidate && effectiveDeclaration is PsiMethod
          else -> effectiveDeclaration !is KtNamedFunction && effectiveDeclaration !is PsiMethod
        }
  }

  private fun isFunction(psiElement: PsiElement): Boolean {
    return when (psiElement) {
      is PsiMethod -> true
      is KtNamedFunction -> true
      else -> false
    }
  }
}
