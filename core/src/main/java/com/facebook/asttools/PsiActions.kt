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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

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
