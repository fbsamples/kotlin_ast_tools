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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

/**
 * AElement is the top of a hierarchy of AST represnetation that merges Java Psi Classes with Kotlin
 * Psi Classes (KtElements).
 *
 * AElements's aim is to allow writing codemods that handle Java and Kotlin at the same time, with
 * code that is as similar as possible to Psi code handling Java or Kotlin.
 *
 * You can look at [com.facebook.tools.codemods.aelements.AElementTest] for a simple example of how
 * to use these classes or other places in the codebase to jumpstarts you work.
 *
 * For any help, post questions in https://fb.workplace.com/groups/mobilecodemods
 */
interface AElement {

  /** The underlying PsiElement that this AElement wraps. */
  val psiElement: PsiElement

  /** The language of the underlying PsiElement. */
  val language: Language
    get() = if (psiElement is KtElement) Language.KOTLIN else Language.JAVA

  /** The underlying PsiElement if the language is Kotlin, null otherwise. */
  val javaElement: PsiElement?
    get() = castJavaElement()

  /** The underlying PsiElement if the language is Java, null otherwise. */
  val kotlinElement: PsiElement?
    get() = castKotlinElement()

  /**
   * Allows users to to handle Java and Kotlin code sepeartely for this one element
   *
   * Usage looks like this:
   * ```
   * myAElement.ifLanguage(
   *   isJava = { ... },
   *   isKotlin = { ... },
   * )
   * ```
   *
   * The [Cases.apply] method takes one lambda for each language. These lambdas will each be called
   * with the correct static type expected for the language. (For example, an [AExpression] will
   * split into a [PsiExpression] or [KtExpression])
   */
  val ifLanguage: Cases<out PsiElement, out PsiElement>
    get() = castIfLanguage()

  val text: String
    get() = psiElement.text

  val parent: AElement?
    get() = psiElement.parent?.toAElement()
}

/** Finds all elements of the given type and matching the predicate and puts them in a list */
inline fun <reified T : AElement> AElement.collectDescendantsOfType(
    noinline predicate: (T) -> Boolean = { true }
): List<T> {
  return psiElement
      .collectDescendantsOfType<PsiElement> {
        it.toAElement() is T && predicate(it.toAElement() as T)
      }
      .map { it.toAElement() } as List<T>
}

/** Finds the first element of the given type and matching the predicate */
inline fun <reified T : AElement> AElement.findDescendantOfType(
    noinline predicate: (T) -> Boolean = { true }
): T? {
  return psiElement
      .findDescendantOfType<PsiElement> {
        val toAElement = it.toAElement()
        toAElement is T && predicate(toAElement as T)
      }
      ?.toAElement() as T?
}

/** Iterates over all elements of the given type */
inline fun <reified T : AElement> AElement.forEachDescendantOfType(noinline block: (T) -> Unit) {
  psiElement.forEachDescendantOfType<PsiElement> {
    val aElement = it.toAElement()
    if (aElement is T) {
      block(aElement)
    }
  }
}

/**
 * Returns the first parent of the given type
 *
 * @param strict If true, will skip testing the given element. If false, the given element can be
 *   returned if it matches
 */
inline fun <reified T : AElement> AElement.getParentOfType(
    strict: Boolean = true,
): T? {
  return (if (strict) psiElement.parents else psiElement.parentsWithSelf)
      .firstOrNull { it.toAElement() is T }
      ?.toAElement() as T?
}

/**
 * Returns a sequence of all ancestors nodes of the given node of the request type, nodes are in
 * order where the immediate parent is before a grandparent
 *
 * @param withSelf If true, the given element will be included in the sequence
 */
inline fun <reified T : AElement> AElement.parentsOfType(
    withSelf: Boolean = false,
): Sequence<T> {
  return psiElement
      .let { if (withSelf) it.parentsWithSelf else it.parents }
      .map { it.toAElement() }
      .filterIsInstance<T>()
}

/**
 * Returns the first parent of the given type
 *
 * @param strict If true, will skip testing the given element. If false, the given element can be
 *   returned if it matches
 */
inline fun <reified T : AElement> AElement.getTopmostParentOfType(): T? {
  return psiElement.parents.lastOrNull { it.toAElement() is T }?.toAElement() as T?
}

open class AElementImpl(override val psiElement: PsiElement) : AElement {
  override fun equals(other: Any?): Boolean = other is AElement && psiElement == other.psiElement

  override fun hashCode(): Int = psiElement.hashCode()

  override fun toString(): String = "${javaClass.simpleName}{text = \"$text\"}"
}

enum class Language {
  JAVA,
  KOTLIN
}

/**
 * Internal representation to allow handling per language of the Psi Element stored in an AElement
 */
class Cases<J, K>(val java: J?, val kotlin: K?) {
  inline operator fun <T> invoke(isJava: (J) -> T, isKotlin: (K) -> T): T =
      when {
        java != null -> isJava(java)
        else -> isKotlin(checkNotNull(kotlin))
      }
}

/** Implements [AElement.javaElement] with automatic casting for shorter subclass code */
internal fun <J> AElement.castJavaElement(): J? =
    (if (language == Language.JAVA) psiElement else null) as? J

/** Implements [AElement.kotlinElement] with automatic casting for shorter subclass code */
internal fun <K> AElement.castKotlinElement(): K? =
    (if (language == Language.KOTLIN) psiElement else null) as? K

/** Implements [AElement.ifLanguage] with automatic casting for shorter subclass code */
internal fun <J, K> AElement.castIfLanguage(): Cases<out J, out K> =
    Cases(castJavaElement(), castKotlinElement())
