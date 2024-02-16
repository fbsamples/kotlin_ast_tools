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

package com.facebook.kotlin.matching

import com.google.errorprone.annotations.CheckReturnValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
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
      matcher: PsiAstMatcher<out T>
  ) {
    matcherFunctions += {
      val t: T? = transform(it)
      if (t == null) null else matcher.matches(t)
    }
  }

  /**
   * Adds a new child matcher that needs to be satisfied for this matcher to be satisfied
   *
   * Similar to [addChildMatcher] but the transform is expected to return a list, and the matcher
   * will be satisfied using a strategy of matching subsequences as in [matchAllInOrder]
   */
  internal fun <T : Any> addIndexedMatchersList(
      transform: (Element) -> List<T>,
      list: List<Pair<Index, PsiAstMatcher<T>>>
  ) {
    matcherFunctions += { matchAllIndexed(list, transform(it)) }
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
 * Helper method to match a list of indexed matchers with a list of all nodes
 *
 * This will try to satisfy each matcher in order, and once matched will removed the node it matched
 * from for future matchers. This means the order of the matchers is meaningful.
 */
internal fun <T : Any> matchAllIndexed(
    indexedMatchers: List<Pair<Index, PsiAstMatcher<T>>>,
    nodes: List<T>
): Map<String, String>? {
  val variableMatches = mutableMapOf<String, String>()
  for (indexedMatcher in indexedMatchers) {
    val index = indexedMatcher.first
    if (!index.isValid(nodes)) {
      return null
    }
    val matcher = indexedMatcher.second
    val nodesToMatch = index.getValue(nodes)
    var hasMatch = false
    for (nodeToMatch in nodesToMatch) {
      val match = matcher.matches(nodeToMatch) ?: continue
      variableMatches.putAll(match)
      hasMatch = true
      break
    }
    if (!hasMatch) {
      return null
    }
  }
  return variableMatches
}

@CheckReturnValue
fun <Element : PsiElement> PsiElement.findAllWithVariables(
    matcher: PsiAstMatcher<Element>
): List<Pair<Element, Map<String, String>>> {
  val results = mutableListOf<Pair<Element, Map<String, String>>>()
  this.accept(
      object : KtTreeVisitorVoid() {
        override fun visitElement(element: PsiElement) {
          val result = matcher.matches(element)
          if (result != null) {
            results.add(Pair(element as Element, result))
          }
          super.visitElement(element)
        }
      })
  return results
}

/**
 * Finds all the elements matching the given matcher inside under this element
 *
 * @see [PsiAstMatcher]
 */
@CheckReturnValue
fun <Element : PsiElement> PsiElement.findAll(matcher: PsiAstMatcher<Element>): List<Element> {
  return findAllWithVariables(matcher).map { it.first }
}

/** Finds and replaces elements in a Kotlin file using a [PsiAstMatcher] */
@CheckReturnValue
fun <Element : PsiElement> KtFile.replaceAll(
    matcher: PsiAstMatcher<Element>,
    replaceWith: (Element) -> String
): KtFile = replaceAllWithVariables(matcher, replaceWith = { (result, _) -> replaceWith(result) })

/**
 * Finds and replaces elements in a Kotlin file using a [PsiAstMatcher]
 *
 * Some elements may intersect, making this complicated. In such cases we try converting outer
 * elements first, then rerun the matcher. If we detect any conversion created new matches, we abort
 * and throw just to make sure we don't do something weird, or go into a infinite loop.
 *
 * This abort mechanism is limited, is some instances remove future instances, and others generate
 * new instances we can be thrown off.
 */
@CheckReturnValue
fun <Element : PsiElement> KtFile.replaceAllWithVariables(
    matcher: PsiAstMatcher<Element>,
    replaceWith: (Pair<Element, Map<String, String>>) -> String
): KtFile {

  var currentPsiFile = this
  var remainingMatches = Int.MAX_VALUE
  while (remainingMatches > 0) {
    val elements: List<Pair<Element, Map<String, String>>> =
        currentPsiFile.findAllWithVariables(matcher)
    if (elements.isEmpty()) {
      return this
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
    currentPsiFile = currentPsiFile.replaceAllWithVariables(nonIntersectingElements, replaceWith)
  }
  return currentPsiFile
}

/** Replaces match results using a transform function */
@CheckReturnValue
fun <Element : PsiElement> KtFile.replaceAllWithVariables(
    elements: List<Pair<Element, Map<String, String>>>,
    replaceWith: (Pair<Element, Map<String, String>>) -> String
): KtFile {
  if (elements.isEmpty()) {
    return this
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

  var text = this.text
  for ((element, replacement) in sortedPatches.reversed()) {
    text = text.substring(0, element.startOffset) + replacement + text.substring(element.endOffset)
  }
  return load(text)
}
