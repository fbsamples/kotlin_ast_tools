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

package com.facebook.asttools

import com.google.errorprone.annotations.CheckReturnValue
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.nextSiblingOfSameType
import org.jetbrains.kotlin.psi.psiUtil.prevSiblingOfSameType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Replaces one element in a KtFile and reloads it
 *
 * Note that the returned file is newly parsed from text, so any reference to an element with it
 * needs to be recomputed as it will be pointing to an older KtFile AST.
 */
@CheckReturnValue
fun <Element : PsiElement> KtFile.replace(element: Element, replacement: String): KtFile {
  var text = this.text
  text = text.substring(0, element.startOffset) + replacement + text.substring(element.endOffset)
  return KotlinParserUtil.parseAsFile(text)
}

/**
 * Replaces all elements matching the given `matcher` predicate with the result of the `replaceWith`
 * transform
 */
@CheckReturnValue
inline fun <reified Element : PsiElement> KtFile.replaceAll(
    noinline matcher: (Element) -> Boolean,
    replaceWith: (Element) -> String
): KtFile {
  val elements = collectDescendantsOfType(matcher)
  if (elements.isEmpty()) {
    return this
  }
  return replaceAll(elements, elements.map { replaceWith(it) })
}

/**
 * Replaces multiple given nodes in a Kotlin file KtFile object with the given replacements
 *
 * @param ktFile the Kotlin file being edited
 * @param elements a list of Kotlin AST node from the KtFile
 * @param replacements the strings to replace the text of the node, with index matching [elements]
 */
@CheckReturnValue
fun KtFile.replaceAll(elements: List<PsiElement>, replacements: List<String>): KtFile {
  return KotlinParserUtil.parseAsFile(replaceElements(text, elements, replacements))
}

/**
 * Remove the matching nodes from the file
 *
 * This is meant to allow removing more complicated nodes where their removal can cause problems.
 * For example, to remove a parameter from a list one needs to sometime remove a comma with it, and
 * sometimes not. This function handles such nodes properly.
 */
@CheckReturnValue
inline fun <reified Element : PsiElement> KtFile.removeAll(
    noinline matcher: (Element) -> Boolean,
): KtFile = removeAll(collectDescendantsOfType(matcher))

/** See [removeAll] */
@CheckReturnValue
fun <Element : PsiElement> KtFile.removeAll(elements: List<Element>): KtFile {
  if (elements.isEmpty()) {
    return this
  }

  val sortedPatches =
      elements.map { result ->
        when (result) {
          is KtParameter -> {
            val ktParameterList = result.parent as KtParameterList
            val nextParameter = result.nextSiblingOfSameType<KtParameter>()
            val prevParameter = result.prevSiblingOfSameType<KtParameter>()
            when {
              nextParameter != null -> Pair(result.startOffset, nextParameter.startOffset)
              prevParameter != null -> Pair(prevParameter.endOffset, result.endOffset)
              else ->
                  Pair(
                      checkNotNull(ktParameterList.leftParenthesis).endOffset,
                      checkNotNull(ktParameterList.rightParenthesis).startOffset)
            }
          }
          is KtAnnotationEntry,
          is KtProperty,
          is KtExpression -> Pair(result.startOffset, result.endOffset)
          else -> TODO("Unsupported type, add code to make it work")
        }
      }

  var text = this.text
  for ((start, end) in sortedPatches.reversed()) {
    text = text.substring(0, start) + text.substring(end)
  }
  return KotlinParserUtil.parseAsFile(text)
}
