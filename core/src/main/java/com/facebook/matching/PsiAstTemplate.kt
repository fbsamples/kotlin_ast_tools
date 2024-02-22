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

import com.facebook.asttools.KotlinParserUtil
import com.intellij.psi.PsiElement
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
 * Returns a list of all expressions in a Kotlin file that match the given string template.
 *
 * For example, the template `#a#.apply(#b#)` will return all `KtExpression` nodes that are of a
 * qualified method call where the method is name `apply` and takes one argument.
 *
 * For each variable in the template an extra matcher can be define under `variables` to allow more
 * accurate matching.
 *
 * See [com.facebook.matching.PsiAstTemplateTest] for a lot of examples using these templates
 */
fun KtFile.findAllExpressions(
    template: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): List<KtExpression> {
  return findAll(parseTemplateWithVariables<KtExpression>(template, *variables))
}

/**
 * Replaces all expressions that match the given template with the given replacement
 *
 * See [findAllExpressions] for more details on the template
 */
fun KtFile.replaceAllExpressions(
    template: String,
    replaceWith: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): KtFile = replaceAllExpressions(template, { _, _ -> replaceWith }, *variables)

/**
 * Replaces all expressions that match the given template with the given replacement which is given
 * as a lambda
 *
 * Use this version instead of [replaceAllExpressions] for cases in which the replacement depends on
 * the actual matched expression
 */
fun KtFile.replaceAllExpressions(
    template: String,
    replaceWith: (match: KtExpression, templateVariablesToText: Map<String, String>) -> String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): KtFile {
  return replaceAllWithVariables(parseTemplateWithVariables<KtExpression>(template, *variables)) {
      (match, templateVariablesToText) ->
    parseReplacementTemplate(
        template, replaceWith(match, templateVariablesToText), templateVariablesToText)
  }
}

/**
 * Like [findAllExpressions] but instead matches on property declarations (i.e. `val a = 5`)
 *
 * See [com.facebook.matching.PsiAstTemplateTest] for a lot of examples using these templates
 */
fun KtFile.findAllProperties(
    template: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): List<KtProperty> {
  val matcher: PsiAstMatcher<KtProperty> =
      parseTemplateWithVariables<KtProperty>(template, *variables)
  return findAll(matcher)
}

/**
 * Like [findAllExpressions] but instead matches on anontations (i.e. `@Magic(param1 = 5`)
 *
 * See [com.facebook.matching.PsiAstTemplateTest] for a lot of examples using these templates
 */
fun KtFile.findAllAnnotations(
    template: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): List<KtAnnotationEntry> {
  return findAll(parseTemplateWithVariables<KtAnnotationEntry>(template, *variables))
}

/**
 * Takes a template string and an optional list of matchers per variavle and builds a
 * [PsiAstMatcher] for that template
 */
inline fun <reified T : Any> parseTemplateWithVariables(
    template: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): PsiAstMatcher<T> {
  val unusedVariables = variables.toMap(mutableMapOf())

  val templateVariables =
      TEMPLATE_VARIABLE_REGEX.findAll(template)
          .map { v ->
            PsiAstTemplate.Variable(
                v.value.removeSurrounding("#"), unusedVariables.remove(v.value) ?: ANY_SENTINEL)
          }
          .toList()
  check(unusedVariables.isEmpty()) {
    "The following variables were not found in the template: " +
        unusedVariables.keys.joinToString(separator = ", ")
  }
  check(templateVariables.map { it.name }.toSet().size == templateVariables.size) {
    "Multiple reference to the same template variable are not supported yet"
  }

  val newTemplate =
      templateVariables.fold(template) { template, variable ->
        template.replace("#${variable.name}#", variable.toString())
      }
  return PsiAstTemplate(templateVariables).parse(T::class.java, newTemplate)
}

fun parseReplacementTemplate(
    template: String,
    replacement: String,
    templateVariablesToText: Map<String, String>,
): String {
  var processedReplacment = replacement
  "#[A-Za-z0-9_]+#".toRegex().findAll(template).forEach { matchResult ->
    val target = matchResult.value
    val variableValue =
        templateVariablesToText[matchResult.value.removeSurrounding("#")]
            ?: error(
                "undeclared variable ${matchResult.value}, known variables: ${templateVariablesToText.keys}")
    processedReplacment = processedReplacment.replace(target, variableValue)
  }
  return processedReplacment
}

/** Scope object to allow template building reference local matchers as arguments */
class PsiAstTemplate(variables: List<Variable<*>> = listOf()) {

  private val variableNamesToVariables: MutableMap<String, Variable<*>> =
      variables.associateByTo(mutableMapOf()) { it.name }

  fun <T : Any> parse(clazz: Class<T>, template: String): PsiAstMatcher<T> {
    return when (clazz) {
      KtProperty::class.java -> parseKotlinRecursive(KotlinParserUtil.parseAsProperty(template))
      KtExpression::class.java -> parseKotlinRecursive(KotlinParserUtil.parseAsExpression(template))
      KtAnnotationEntry::class.java ->
          parseKotlinRecursive(KotlinParserUtil.parseAsAnnotationEntry(template))
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
  private fun <T : Any> parseKotlinRecursive(node: T): PsiAstMatcher<T> {
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
                      transform = { it.delegateExpression }, matcher = parseKotlinRecursive(it))
                }
                node.initializer?.let {
                  addChildMatcher(
                      transform = { it.initializer }, matcher = parseKotlinRecursive(it))
                }
              }
      // for example: `doIt(1, b)`
      is KtCallExpression ->
          match<KtCallExpression>().apply {
            node.referenceExpression()?.let { referenceExpression ->
              addChildMatcher(
                  { it.referenceExpression() }, parseKotlinRecursive(referenceExpression))
            }
            if (!isSelectorExpression(node)) {
              addCustomMatcher { !(isSelectorExpression(it)) }
            }
            addIndexedMatchersList(
                { it.valueArguments },
                node.valueArguments.withIndex().map { indexedValue ->
                  Pair(Index.at(indexedValue.index), parseKotlinRecursive(indexedValue.value))
                })
            addChildMatcher { it.valueArguments.size == node.valueArguments.size }
          }
      is KtQualifiedExpression ->
          match<KtQualifiedExpression>().apply {
            addChildMatcher(
                { it.receiverExpression }, parseKotlinRecursive(node.receiverExpression))
            addChildMatcher { it.operationSign.value == node.operationSign.value }
            node.selectorExpression?.let { selectorExpression ->
              addChildMatcher({ it.selectorExpression }, parseKotlinRecursive(selectorExpression))
            }
          }
      is KtClassLiteralExpression ->
          match<KtClassLiteralExpression>().apply {
            node.receiverExpression?.let { expression ->
              addChildMatcher({ it.receiverExpression }, parseKotlinRecursive(expression))
            }
          }
      is KtUnaryExpression -> {
        match<KtUnaryExpression>().apply {
          addChildMatcher(
              { it.baseExpression }, parseKotlinRecursive(checkNotNull(node.baseExpression)))
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
      // for example: `1` in `doIt(1, b)`
      is KtValueArgument ->
          match<KtValueArgument>().apply {
            node.getArgumentExpression()?.let { expression ->
              addChildMatcher({ it.getArgumentExpression() }, parseKotlinRecursive(expression))
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
    val matcherFromVariable = variableNamesToVariables[varName]?.matcher
    if (matcherFromVariable == ANY_SENTINEL) {
      return match<T>().also { it.variableName = varName }
    }
    matcherFromVariable
        ?: error(
            "Template references variable $$varName, " +
                "but one was defined: Add `val $varName by match<...> { ... }` to your template")
    check(T::class.java == matcherFromVariable.targetType) {
      "Template references variable $$varName as a matcher of ${T::class.java.simpleName}, " +
          "but variable was defined as a matcher of ${matcherFromVariable.targetType.simpleName}"
    }
    return matcherFromVariable as PsiAstMatcher<T>
  }

  class Variable<T : Any>(val name: String, val matcher: PsiAstMatcher<T>) {
    init {
      matcher.variableName = name
    }

    override fun toString(): String {
      return "`$$name$`"
    }
  }
}

val ANY_SENTINEL: PsiAstMatcher<PsiElement> = PsiAstMatcher(PsiElement::class.java)
val TEMPLATE_VARIABLE_REGEX: Regex = "#[A-Za-z0-9_]+#".toRegex()
