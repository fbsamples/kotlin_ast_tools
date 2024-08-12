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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Inserts the method after all other properties/methods and returns the resulting text. If a method
 * with the same signature is already present, the new method will be added anyway. For example,
 * given a class like
 *
 * ```
 * class Foo(val name: String) {
 *   fun getFoo(name: String): Foo = Foo(name)
 * }
 * ```
 *
 * and method text `fun getFoo(a: String): Foo = Foo(a)`, this method would the original class text
 */
fun KtClassOrObject.withFunction(methodText: String): KtClassOrObject {
  val updatedClassText =
      when {
        this.body == null -> "${this.text} {\n  $methodText\n}"
        this.declarations.isEmpty() ->
            this.text.replaceAfterLast("}", "").removeSuffix("}") + "\n  $methodText\n}"
        else ->
            replaceNodeInAncestor(
                this, this.declarations.last(), "${this.declarations.last().text}\n\n  $methodText")
      }
  return KotlinParserUtil.parseAsClassOrObject(updatedClassText)
}

/**
 * Replaces multiple given nodes in a any code (kotlin or java) with the given replacements
 *
 * @param code the Kotlin/Java code to edit
 * @param elements a list of AST node from the file
 * @param replacements the strings to replace the text of the node, with index matching [elements]
 */
fun replaceElements(code: String, elements: List<PsiElement>, replacements: List<String>): String {
  if (elements.size != replacements.size) {
    throw IllegalArgumentException(
        "'elements' (${elements.size}) and replacements (${replacements.size}) have different lengths")
  }
  val sortedPatches: List<Pair<PsiElement, String>> =
      elements.zip(replacements).sortedBy { it.first.endOffset }.sortedBy { it.first.startOffset }

  var previousPatchEndOffset = -1
  for (patch in sortedPatches) {
    if (patch.first.startOffset < previousPatchEndOffset) {
      val patchesDescription =
          sortedPatches.joinToString(separator = "\n", prefix = "\n") {
            (if (it == patch) "**** " else "") +
                "Patch: ${it.first.javaClass}" +
                " from: ${it.first.startOffset}" +
                " to: ${it.first.endOffset}" +
                " original text: ${it.first.text}" +
                " replacement: ${it.second}"
          }
      throw IllegalArgumentException("Cannot apply patches, patches intersect:$patchesDescription")
    }
    previousPatchEndOffset = patch.first.endOffset
  }

  var text = code
  for ((element, replacement) in sortedPatches.reversed()) {
    text = text.substring(0, element.startOffset) + replacement + text.substring(element.endOffset)
  }
  return text
}

private fun replaceNodeInAncestor(
    parentNode: PsiElement,
    node: PsiElement,
    replacement: String
): String {
  val parentText = parentNode.text
  val parentStartOffset = parentNode.startOffset
  val relativeStartOffset = node.startOffset - parentStartOffset
  val relativeEndOffset = node.endOffset - parentStartOffset - 1
  return parentText.substring(0, relativeStartOffset) +
      replacement +
      if (relativeEndOffset < parentText.lastIndex) parentText.substring(relativeEndOffset + 1)
      else ""
}
