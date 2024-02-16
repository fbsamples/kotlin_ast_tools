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

import com.facebook.kotlin.asttools.KotlinParserUtil
import com.intellij.psi.PsiElement
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression

/**
 * Create a new [PsiAstMatcher] using a code template
 *
 * For example, the following template call:
 * ```
 * template { "doIt(1 + 1)" }
 * ```
 *
 * Would create a matcher looking for an expression of a method call, with doIt as the name, and the
 * value argument as a binary expression of 1 + 1.
 */
inline fun <reified T : Any> template(block: PsiAstTemplate.() -> String): PsiAstMatcher<T> {
  return template(T::class.java, block)
}

/** Like [template], but without the reified argument so it can be called from Java code */
inline fun <T : Any> template(
    clazz: Class<T>,
    block: PsiAstTemplate.() -> String
): PsiAstMatcher<T> {
  val psiAstTemplate = PsiAstTemplate()
  val template = psiAstTemplate.block()
  return psiAstTemplate.parse(clazz, template)
}

fun KtFile.findAllExpressions(template: String): List<KtExpression> {
  return findAll(PsiAstTemplate().parse(KtExpression::class.java, template))
}

fun KtFile.findAllProperties(template: String): List<KtProperty> {
  return findAll(PsiAstTemplate().parse(KtProperty::class.java, template))
}

fun KtFile.findAllAnnotations(template: String): List<KtAnnotationEntry> {
  return findAll(PsiAstTemplate().parse(KtAnnotationEntry::class.java, template))
}

/** Scope object to allow template building reference local matchers as arguments */
class PsiAstTemplate {

  private val variableNamesToVariables: MutableMap<String, Variable<*>> = mutableMapOf()

  /** Future construct to allow variables */
  operator fun getValue(nothing: Nothing?, property: KProperty<*>): String {
    return "`$" + property.name + "$`"
  }

  /**
   * Refer to this in a template for a wildcard, for example "val ${any} = 1" would match any
   * declaration where the initializer is 1, but we do not care about the name of the variable
   */
  val any: String = "`$$`"

  fun <T : Any> parse(clazz: Class<T>, template: String): PsiAstMatcher<T> {
    return when (clazz) {
      KtProperty::class.java -> parseRecursive(KotlinParserUtil.parseAsProperty(template))
      KtExpression::class.java -> parseRecursive(KotlinParserUtil.parseAsExpression(template))
      KtAnnotationEntry::class.java ->
          parseRecursive(KotlinParserUtil.parseAsAnnotationEntry(template))
      else -> error("unsupported: $clazz")
    }
        as PsiAstMatcher<T>
  }

  /**
   * Parses a Kt AST node, and returns a [link KtAstMatcher] that fits it. This is done recursively
   * and can be quite a mess to read.
   *
   * The basic handling is that for every interesting type we take the important fields from it, for
   * example for a property, we will take its name, type, etc. and parse each of those recursively.
   *
   * For the edge case, we compare on the text of the node, or see if it represents a template
   * variable which means its matcher is defined outside the template.
   */
  private fun <T : Any> parseRecursive(node: T): PsiAstMatcher<T> {
    return when (node) {
      // for example: `private val foo: Foo = Foo(5)`
      is KtProperty ->
          loadIfVariableOr(node.nameIdentifier?.text) {
                match<KtProperty>().apply {
                  node.nameIdentifier?.text?.let {
                    addChildMatcher { property -> property.name == it }
                  }
                }
              }
              .apply {
                node.delegateExpression?.let {
                  addChildMatcher(
                      transform = { it.delegateExpression }, matcher = parseRecursive(it))
                }
                node.initializer?.let {
                  addChildMatcher(transform = { it.initializer }, matcher = parseRecursive(it))
                }
              }
      // for example: `doIt(1, b)`
      is KtCallExpression ->
          match<KtCallExpression>().apply {
            node.referenceExpression()?.let { referenceExpression ->
              addChildMatcher({ it.referenceExpression() }, parseRecursive(referenceExpression))
            }
            if (!isSelectorExpression(node)) {
              addCustomMatcher { !(isSelectorExpression(it)) }
            }
            addIndexedMatchersList(
                { it.valueArguments },
                node.valueArguments.withIndex().map { indexedValue ->
                  Pair(Index.at(indexedValue.index), parseRecursive(indexedValue.value))
                })
            addChildMatcher { it.valueArguments.size == node.valueArguments.size }
          }
      is KtQualifiedExpression ->
          match<KtQualifiedExpression>().apply {
            addChildMatcher({ it.receiverExpression }, parseRecursive(node.receiverExpression))
            addChildMatcher { it.operationSign.value == node.operationSign.value }
            node.selectorExpression?.let { selectorExpression ->
              addChildMatcher({ it.selectorExpression }, parseRecursive(selectorExpression))
            }
          }
      is KtClassLiteralExpression ->
          match<KtClassLiteralExpression>().apply {
            node.receiverExpression?.let { expression ->
              addChildMatcher({ it.receiverExpression }, parseRecursive(expression))
            }
          }
      is KtUnaryExpression -> {
        match<KtUnaryExpression>().apply {
          addChildMatcher({ it.baseExpression }, parseRecursive(checkNotNull(node.baseExpression)))
          addChildMatcher { it is KtPostfixExpression == node is KtPostfixExpression }
          addChildMatcher { it.operationReference.text == node.operationReference.text }
        }
      }
      // any expression for which we don't have more specific handling, such as `1`, or `foo`
      is KtExpression ->
          loadIfVariableOr(node.text) {
            match<KtExpression>().apply {
              addChildMatcher { expression -> expression.text == node.text }
            }
          }
      // for example: `doIt(1, b)`
      is KtValueArgument ->
          match<KtValueArgument>().apply {
            node.getArgumentExpression()?.let { expression ->
              addChildMatcher({ it.getArgumentExpression() }, parseRecursive(expression))
            }
            node.getArgumentName()?.asName?.identifier?.let { identifier ->
              addChildMatcher { it.getArgumentName()?.asName?.identifier == identifier }
            }
          }
      // for example: `@Magic` or `@Foo(1)`
      is KtAnnotationEntry ->
          loadIfVariableOr(node.shortName?.identifier) {
            match<KtAnnotationEntry>().apply {
              node.shortName?.identifier?.let { identifier ->
                addChildMatcher { it.shortName?.identifier == identifier }
              }
            }
          }
      else -> error("unsupported: ${node.javaClass}")
    }
        as PsiAstMatcher<T>
  }

  /**
   * true if this expression is a selector in a qualified expression, for example `doIt()` in
   * `foo.doIt()`
   */
  private fun isSelectorExpression(ktExpression: KtExpression) =
      (ktExpression.parent as? KtDotQualifiedExpression)?.selectorExpression == ktExpression

  /** Checks is an identifier is a marker for a variable from the from `$foo$` */
  private fun isVarName(string: String?) =
      string != null && string.startsWith("`$") && string.endsWith("$`")

  /**
   * For a given node, check if it represents a variable such as $name$, and if so, load it and
   * return the appropriate matcher (or throw is something is suspicious), otherwise, execute the
   * given code block which is supposed to parse the matcher
   */
  private inline fun <reified T : PsiElement> loadIfVariableOr(
      textContent: String?,
      ifNotVariableBlock: () -> PsiAstMatcher<T>
  ): PsiAstMatcher<T> {
    if (textContent == null || !isVarName(textContent)) {
      return ifNotVariableBlock()
    }

    val varName = textContent.removePrefix("`$").removeSuffix("$`")
    if (varName.isEmpty()) {
      return match()
    }
    val matcherFromVariable = variableNamesToVariables[varName]?.matcher
    matcherFromVariable
        ?: error(
            "Template references variable $$varName, " +
                "but one was defined: Add `val $varName by match<...> { ... }` to your template")
    check(matcherFromVariable.targetType == T::class.java) {
      "Template references variable $$varName as a matcher of ${T::class.java.simpleName}, " +
          "but variable was defined as a matcher of ${matcherFromVariable.targetType.simpleName}"
    }
    return matcherFromVariable as PsiAstMatcher<T>
  }

  /** Called by `val a by match<*> { ... }` and registers a new matcher with a variable name `a` */
  operator fun <T : Any> PsiAstMatcher<T>.getValue(
      nothing: Nothing?,
      property: KProperty<*>
  ): Variable<T> {
    this.variableName = property.name
    val ktTemplateVariable = Variable(property.name, this)
    variableNamesToVariables[property.name] = ktTemplateVariable
    return ktTemplateVariable
  }

  class Variable<T : Any>(val name: String, val matcher: PsiAstMatcher<T>) {
    override fun toString(): String {
      return "`$$name$`"
    }
  }
}
