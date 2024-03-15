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

import com.google.errorprone.annotations.CheckReturnValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Create a new matcher with the given predicate
 *
 * Additional conditions can be then added with [PsiAstMatcher.addCustomMatcher]
 */
inline fun <reified T : Any> match(
    noinline predicate: (T) -> Boolean = { true }
): PsiAstMatcher<T> {
  val matcher = PsiAstMatcher(T::class.java)
  matcher.addCustomMatcher(predicate)
  return matcher
}

/** Set this to true to print debug messages on what comparisons succeed and which ones do not */
var enableKtAstMatcherDebugPrints = false

/**
 * Kotlin nodes matcher
 *
 * In order to avoid the need to create this class per every type it is built in a generic way
 * storing a list of children matchers.
 *
 * Create one of these matchers using [match]
 */
class PsiAstMatcher<Element : Any>(internal val targetType: Class<Element>) {

  /**
   * Stores all the match conditions that need to be satisfied
   *
   * The key is a string that will be removed soon
   *
   * The value is method that takes our Element, and returns a non null result for a match (which
   * serves as the "proof" for the match), or a null in case of no match
   */
  private val matcherFunctions: MutableList<(Element) -> Map<String, String>?> = mutableListOf()

  /** A name for this matcher that may be used to retrieve its result later */
  internal var variableName: String? = null

  /**
   * Whether this matcher will match a null ot not, this is useful for building optional matchers
   */
  internal var shouldMatchToNull: Boolean = false

  /**
   * Finds all the elements matching the given matcher inside under this element
   *
   * @see [PsiAstMatcher]
   */
  @CheckReturnValue
  fun findAll(element: PsiElement): List<Element> {
    return findAllWithVariables(element).map { it.first }
  }

  @CheckReturnValue
  fun findAllWithVariables(element: PsiElement): List<Pair<Element, Map<String, String>>> {
    val results = mutableListOf<Pair<Element, Map<String, String>>>()
    element.accept(
        object : KtTreeVisitorVoid() {
          override fun visitElement(element: PsiElement) {
            val result = matches(element)
            if (result != null) {
              results.add(Pair(element as Element, result))
            }
            super.visitElement(element)
          }
        })
    return results
  }

  /**
   * Adds a new child matcher that needs to be satisfied for this matcher to be satisfied
   *
   * This is used by the various extension properties to define what useful child matchers each node
   * can support. If you are no extending this framework, you can use [addCustomMatcher] to add one
   * off matchers for your use case.
   *
   * @param key the name by which this matcher's result can later be found
   * @param transform extracts a value of type T from an Element, which will then be checked by
   *   predicate (for example: to match on return type of a function, we need given a
   *   KtNamedFunction give back the node for the return type)
   * @param predicate the predicate to satisfy for the child matcher
   */
  internal fun <T> addChildMatcher(
      transform: (Element) -> T?,
      predicate: (T) -> Boolean,
  ) {
    matcherFunctions += {
      val t: T? = transform(it)
      if (t != null && predicate(t)) mapOf() else null
    }
  }

  /**
   * Adds a new child matcher from a predicate that needs to be satisfied for this matcher to be
   * satisfied
   *
   * This method works best if we want another check in the matcher to be done, but it's a simple
   * string comparison. Otherwise child matchers are better added with the version taking a
   * transform and a matcher object
   */
  internal fun addChildMatcher(
      predicate: (Element) -> Boolean,
  ) {
    matcherFunctions += { if (predicate(it)) mapOf() else null }
  }

  /**
   * Adds a new child matcher that needs to be satisfied for this matcher to be satisfied
   *
   * Similar to [addChildMatcher] but takes an actual KtAstMatcher instead of a predicate
   */
  internal fun <T : Any> addChildMatcher(
      transform: (Element) -> T?,
      matcher: PsiAstMatcher<out T>,
      inheritShouldMatchNull: Boolean = false,
  ) {
    matcherFunctions += {
      val t: T? = transform(it)
      if (t == null) null else matcher.matches(t)
    }
    if (inheritShouldMatchNull && matcher.shouldMatchToNull) {
      shouldMatchToNull = true
    }
  }

  /**
   * Adds a new child matcher that needs to be satisfied for this matcher to be satisfied
   *
   * Similar to [addChildMatcher] but the transform is expected to return a list, and the matcher
   * will be satisfied using a strategy of matching subsequences as in [matchAllInOrder]
   */
  internal fun <T : Any> addMatchersInOrderList(
      transform: (Element) -> List<T>,
      list: List<PsiAstMatcher<T>>
  ) {
    matcherFunctions += { matchAllInOrder(list, transform(it)) }
  }

  /**
   * Adds a custom matcher as a child to this matcher
   *
   * See examples in [KtAstMatcherTest]
   */
  fun addCustomMatcher(predicate: (Element) -> Boolean) {
    addChildMatcher({ it }, predicate)
  }

  internal fun matches(obj: Any?): Map<String, String>? {
    if (shouldMatchToNull && obj == null) {
      return mutableMapOf()
    }
    val element = if (targetType.isInstance(obj)) targetType.cast(obj) else return null
    element ?: return null
    val result = mutableMapOf<String, String>()
    for (matcherFunction in matcherFunctions) {
      val childResult = matcherFunction(element)
      if (enableKtAstMatcherDebugPrints) {
        println(
            "KtAstMatcher-debug: Match $matcherFunction " +
                "on ${if (obj is PsiElement) "'${obj.text}'" else obj} -> $childResult")
      }
      childResult ?: return null
      result.putAll(childResult)
    }
    variableName?.let { result[it] = (element as? PsiElement)?.text ?: element.toString() }
    return result
  }

  override fun toString(): String {
    return "KtAstMatcher<${targetType.simpleName}> " +
        "${variableName?.let { "variableName = $variableName "}}" +
        matcherFunctions.joinToString(separator = "\n    ", prefix = "{\n", postfix = "\n}")
  }
}

/**
 * Helper method to match a list of matchers with a list of all nodes
 *
 * This will try to satisfy each matcher in order, and once matched will removed the node it matched
 * from for future matchers. This means the order of the matchers is meaningful. If some matchers
 * are optional, and are unmatched at the end this will still count as a match
 */
internal fun <T : Any> matchAllInOrder(
    matchers: List<PsiAstMatcher<T>>,
    nodes: List<T>
): Map<String, String>? {
  val variableMatches = mutableMapOf<String, String>()
  var matcherIndex = 0
  var nodesIndex = 0
  while (matcherIndex < matchers.size && nodesIndex < nodes.size) {
    val matcher = matchers[matcherIndex]
    val node = nodes[nodesIndex]
    val match = matcher.matches(node)
    if (match == null) {
      if (matcher.matches(null) != null) {
        matcherIndex++
        continue
      } else {
        return null
      }
    } else {
      matcherIndex++
      nodesIndex++
      variableMatches.putAll(match)
    }
  }
  return if (nodesIndex == nodes.size &&
      (matcherIndex until matchers.size).all { index -> (matchers[index].matches(null) != null) }) {
    variableMatches
  } else {
    null
  }
}

@CheckReturnValue
fun <Element : PsiElement, PsiFileType : PsiFile> replaceAllWithVariables(
    psiFile: PsiFileType,
    matcher: PsiAstMatcher<Element>,
    replaceWith: (Pair<Element, Map<String, String>>) -> String,
    reloadFile: (String) -> PsiFileType
): PsiFileType {
  var currentPsiFile: PsiFileType = psiFile
  var remainingMatches = Int.MAX_VALUE
  while (remainingMatches > 0) {
    val elements: List<Pair<Element, Map<String, String>>> =
        matcher.findAllWithVariables(currentPsiFile)
    if (elements.isEmpty()) {
      return psiFile
    }
    if (elements.size > remainingMatches) {
      throw IllegalArgumentException(
          "Cannot apply patches, some patches intersect, and applying them creates new candidates")
    }
    val nonIntersectingElements =
        elements.filter { element ->
          elements.none { anotherElement ->
            anotherElement.first.isAncestor(element.first, strict = true)
          }
        }
    val newRemainingElements = elements.size - nonIntersectingElements.size
    // Guarantee no infinite loop is possible
    if (newRemainingElements >= remainingMatches) {
      throw IllegalArgumentException(
          "Cannot apply patches, some patches intersect, and applying them creates new candidates")
    }
    remainingMatches = newRemainingElements
    currentPsiFile =
        replaceAllWithVariables(currentPsiFile, nonIntersectingElements, replaceWith, reloadFile)
  }
  return currentPsiFile
}

@CheckReturnValue
internal fun <Element : PsiElement, PsiFileType : PsiFile> replaceAllWithVariables(
    psiFile: PsiFileType,
    elements: List<Pair<Element, Map<String, String>>>,
    replaceWith: (Pair<Element, Map<String, String>>) -> String,
    reloadFile: (String) -> PsiFileType,
): PsiFileType {
  if (elements.isEmpty()) {
    return psiFile
  }

  val sortedPatches: List<Pair<PsiElement, String>> =
      elements.map { Pair(it.first, replaceWith(it)) }

  var previousPatchEndOffset = -1
  for (patch in sortedPatches) {
    if (patch.first.startOffset <= previousPatchEndOffset) {
      val patchesDescription =
          sortedPatches.joinToString(separator = "\n", prefix = "\n") {
            "Patch: ${it.first.javaClass} " +
                "from: ${it.first.startOffset} " +
                "to: ${it.first.endOffset} " +
                "replacement: ${it.second}"
          }
      throw IllegalArgumentException("Cannot apply patches, patches intersect:$patchesDescription")
    }
    previousPatchEndOffset = patch.first.endOffset
  }

  var text = psiFile.text
  for ((element, replacement) in sortedPatches.reversed()) {
    text = text.substring(0, element.startOffset) + replacement + text.substring(element.endOffset)
  }
  return reloadFile(text)
}
