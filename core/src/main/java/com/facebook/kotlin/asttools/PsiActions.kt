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

package com.facebook.kotlin.asttools

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getStartOffsetIn
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.startsWithComment

/**
 * Inserts the annotation after the provided PsiElement's leading comment, if any, and returns the
 * resulting text. If an annotation with the same name is already present, returns the original
 * text. For example, given a declaration like
 *
 * ```
 * @Bar("dog") fun doThing() = Unit
 * ```
 *
 * and annotation text `@Foo("cat")`, this method would return
 *
 * ```
 * @Foo("cat") @Bar("dog") fun doThing() = Unit
 * ```
 *
 * However, if the passed annotation text were instead `@Bar("cat")`, this method would return the
 * original declaration's text unchanged.
 */
inline fun <reified T : KtDeclaration> T.withAnnotation(annotationText: String): T =
    this.withAnnotation(KotlinParserUtil.parseAsAnnotationEntry(annotationText))

/**
 * Inserts the annotation after the provided PsiElement's leading comment, if any, and returns the
 * resulting text. If an annotation with the same name is already present, returns the original
 * text. For example, given a declaration like
 *
 * ```
 * @Bar("dog") fun doThing() = Unit
 * ```
 *
 * and annotation text `@Foo("cat")`, this method would return
 *
 * ```
 * @Foo("cat") @Bar("dog") fun doThing() = Unit
 * ```
 *
 * However, if the passed annotation text were instead `@Bar("cat")`, this method would return the
 * original declaration's text unchanged.
 */
inline fun <reified T : KtDeclaration> T.withAnnotation(annotation: KtAnnotationEntry): T {
  return if (this.annotationEntries.any { it.shortName == annotation.shortName }) {
    this
  } else {
    val updatedText =
        if (this.startsWithComment()) {
          val commentChild = this.getChildOfType<KDoc>() ?: PsiTreeUtil.firstChild(this)
          val commentChildOffset = commentChild.getStartOffsetIn(this) + commentChild.textLength
          "${this.text.substring(0, commentChildOffset)}\n${annotation.text} ${this.text.substring(commentChildOffset + 1)}"
        } else {
          "${annotation.text} ${this.text}"
        }
    KotlinParserUtil.parseAsDeclaration(updatedText) as T
  }
}

/**
 * Inserts the supertype after existing supertypes, if any, and returns the resulting text. If a
 * supertype with the same name is already present, returns the original text. For example, given a
 * declaration like
 *
 * ```
 * class Foo(val name: String) : SuperFoo(name)
 * ```
 *
 * and supertype text `Cat`, this method would return
 *
 * ```
 * class Foo(val name: String) : SuperFoo(name), Cat
 * ```
 */
fun KtClassOrObject.withSupertype(supertypeText: String): KtClassOrObject {
  val newSupertypeName =
      KotlinParserUtil.parseAsSupertype(supertypeText)
          .collectDescendantsOfType<KtNameReferenceExpression>()
          .map { it.text }
          .firstOrNull() ?: return this

  if (this.superTypeListEntries.any {
    it.collectDescendantsOfType<KtNameReferenceExpression>().map { it.text }.firstOrNull() ==
        newSupertypeName
  }) {
    return this
  }

  val lastSupertype = this.superTypeListEntries.lastOrNull()
  val classBody = this.body
  val updatedClassText =
      when {
        lastSupertype != null ->
            replaceNodeInAncestor(this, lastSupertype, "${lastSupertype.text}, $supertypeText")
        classBody != null ->
            replaceNodeInAncestor(this, classBody, ": $supertypeText ${classBody.text}")
        else -> "${this.text} : $supertypeText"
      }

  return KotlinParserUtil.parseAsClassOrObject(updatedClassText)
}

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
