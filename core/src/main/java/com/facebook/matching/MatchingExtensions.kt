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

import com.facebook.asttools.JavaPsiParserUtil
import com.facebook.asttools.KotlinParserUtil
import com.google.errorprone.annotations.CheckReturnValue
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

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
  return PsiAstTemplateParser()
      .parseTemplateWithVariables<KtExpression>(template, *variables)
      .findAll(this)
}

fun PsiJavaFile.findAllExpressions(
    template: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): List<PsiExpression> {
  return PsiAstTemplateParser()
      .parseTemplateWithVariables<PsiExpression>(template, *variables)
      .findAll(this)
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

fun PsiJavaFile.replaceAllExpressions(
    template: String,
    replaceWith: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): PsiJavaFile = replaceAllExpressions(template, { _, _ -> replaceWith }, *variables)

/**
 * Replaces all expressions that match the given template with the given replacement which is given
 * as a lambda
 *
 * Use this version instead of [replaceAllExpressions] for cases in which the replacement depends on
 * the actual matched expression
 */
fun KtFile.replaceAllExpressions(
    template: String,
    replaceWith: (match: KtExpression, templateVariablesToText: MatchResult) -> String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): KtFile {
  return replaceAllWithVariables(
      PsiAstTemplateParser().parseTemplateWithVariables<KtExpression>(template, *variables)) {
          (match, templateVariablesToText) ->
        PsiAstTemplateParser()
            .parseReplacementTemplate(
                template, replaceWith(match, templateVariablesToText), templateVariablesToText)
      }
}

fun PsiJavaFile.replaceAllExpressions(
    template: String,
    replaceWith: (match: PsiExpression, templateVariablesToText: MatchResult) -> String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): PsiJavaFile {
  return replaceAllWithVariables(
      PsiAstTemplateParser().parseTemplateWithVariables<PsiExpression>(template, *variables)) {
          (match, templateVariablesToText) ->
        PsiAstTemplateParser()
            .parseReplacementTemplate(
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
      PsiAstTemplateParser().parseTemplateWithVariables<KtProperty>(template, *variables)
  return matcher.findAll(this)
}

fun PsiJavaFile.findAllFields(
    template: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): List<PsiField> {
  return PsiAstTemplateParser()
      .parseTemplateWithVariables<PsiField>(template, *variables)
      .findAll(this)
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
  return PsiAstTemplateParser()
      .parseTemplateWithVariables<KtAnnotationEntry>(template, *variables)
      .findAll(this)
}

fun PsiJavaFile.findAllAnnotations(
    template: String,
    vararg variables: Pair<String, PsiAstMatcher<*>>
): List<PsiAnnotation> {
  return PsiAstTemplateParser()
      .parseTemplateWithVariables<PsiAnnotation>(template, *variables)
      .findAll(this)
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
    replaceWith: (Pair<Element, MatchResult>) -> String
): KtFile {
  return replaceAllWithVariables(
      this, matcher, replaceWith, reloadFile = { text -> KotlinParserUtil.parseAsFile(text) })
}

@CheckReturnValue
fun <Element : PsiElement> PsiJavaFile.replaceAllWithVariables(
    matcher: PsiAstMatcher<Element>,
    replaceWith: (Pair<Element, MatchResult>) -> String
): PsiJavaFile {
  return replaceAllWithVariables(
      this, matcher, replaceWith, reloadFile = { text -> JavaPsiParserUtil.parseAsFile(text) })
}

/** Replaces match results using a transform function */
@CheckReturnValue
fun <Element : PsiElement> KtFile.replaceAllWithVariables(
    elements: List<Pair<Element, MatchResult>>,
    replaceWith: (Pair<Element, MatchResult>) -> String
): KtFile {
  return replaceAllWithVariables(
      this, elements, replaceWith, reloadFile = { text -> KotlinParserUtil.parseAsFile(text) })
}

@CheckReturnValue
fun <Element : PsiElement> PsiJavaFile.replaceAllWithVariables(
    elements: List<Pair<Element, MatchResult>>,
    replaceWith: (Pair<Element, MatchResult>) -> String
): PsiJavaFile {
  return replaceAllWithVariables(
      this, elements, replaceWith, reloadFile = { text -> JavaPsiParserUtil.parseAsFile(text) })
}
