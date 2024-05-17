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

import org.jetbrains.kotlin.psi.KtElement

class PsiAstTemplateParser(val resolver: Resolver = Resolver.DEFAULT) {

  /**
   * Takes a template string and an optional list of matchers per variavle and builds a
   * [PsiAstMatcher] for that template
   */
  inline fun <reified T : Any> parseTemplateWithVariables(
      template: String,
      vararg variables: Pair<String, PsiAstMatcher<*>>
  ): PsiAstMatcher<T> {
    val unusedVariables = variables.toMap(mutableMapOf())

    val rangesToTemplateVariables: List<Pair<IntRange, Variable>> =
        Variable.TEMPLATE_VARIABLE_REGEX.findAll(template)
            .map { v ->
              v.range to
                  Variable(
                      checkNotNull(v.groups["name"]).value,
                      unusedVariables.remove(v.value) ?: Variable.ANY_SENTINEL,
                      isOptional = checkNotNull(v.groups["isOptional"]).value == "?",
                      isKotlin = KtElement::class.java.isAssignableFrom(T::class.java),
                      resolver = resolver,
                      arguments = v.groups["arguments"]?.value)
            }
            .toList()
    val templateVariables = rangesToTemplateVariables.map { it.second }
    check(unusedVariables.isEmpty()) {
      "The following variables were not found in the template: " +
          unusedVariables.keys.joinToString(separator = ", ") +
          "\nVariables found in template: " +
          templateVariables.joinToString { it.templateString }
    }

    check(templateVariables.map { it.name }.toSet().size == templateVariables.size) {
      "Multiple reference to the same template variable are not supported yet"
    }

    val newTemplate =
        rangesToTemplateVariables.reversed().fold(template) { template, variable ->
          template.replaceRange(variable.first, variable.second.parsableCodeString)
        }
    return PsiAstTemplate(templateVariables).parse(T::class.java, newTemplate)
  }

  fun parseReplacementTemplate(
      template: String,
      replacement: String,
      templateVariablesToText: MatchResult,
  ): String {
    var processedReplacment = replacement
    Variable.TEMPLATE_VARIABLE_REGEX.findAll(template).forEach { matchResult ->
      val variableName =
          matchResult.groups["name"]?.value ?: error("Error parsing variable ${matchResult.value}")
      val isOptional = matchResult.groups["isOptional"]?.value == "?"
      val target = "#$variableName#"
      val variableValue =
          templateVariablesToText.getVariableResult(
              matchResult.groups["name"]?.value
                  ?: error("Error parsing variable ${matchResult.value}"))
              ?: if (isOptional) ""
              else
                  error(
                      "undeclared variable ${matchResult.value}, known variables: ${templateVariablesToText.matchedVariables.keys}")
      processedReplacment = processedReplacment.replace(target, variableValue)
    }
    return processedReplacment
  }
}
