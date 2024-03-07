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
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiPostfixExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiUnaryExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression

/** Scope object to allow template building reference local matchers as arguments */
class PsiAstTemplate(variables: List<Variable> = listOf()) {

  private val variableNamesToVariables: MutableMap<String, Variable> =
      variables.associateByTo(mutableMapOf()) { it.name }

  fun <T : Any> parse(clazz: Class<T>, template: String): PsiAstMatcher<T> {
    return when (clazz) {
      KtProperty::class.java -> parseKotlinRecursive(KotlinParserUtil.parseAsProperty(template))
      KtExpression::class.java -> parseKotlinRecursive(KotlinParserUtil.parseAsExpression(template))
      KtAnnotationEntry::class.java ->
          parseKotlinRecursive(KotlinParserUtil.parseAsAnnotationEntry(template))
      PsiExpression::class.java -> parseJavaRecursive(JavaPsiParserUtil.parseAsExpression(template))
      PsiField::class.java -> parseJavaRecursive(JavaPsiParserUtil.parseAsField(template))
      PsiAnnotation::class.java -> parseJavaRecursive(JavaPsiParserUtil.parseAsAnnotation(template))
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
            addMatchersInOrderList(
                { it.valueArguments },
                node.valueArguments.map { valueArgument -> parseKotlinRecursive(valueArgument) })
            addMatchersInOrderList(
                { it.typeArguments },
                node.typeArguments.map { typeProjection -> parseKotlinRecursive(typeProjection) })
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
              addChildMatcher(
                  { it.getArgumentExpression() },
                  parseKotlinRecursive(expression),
                  inheritShouldMatchNull = true)
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
      // for example "Bar<*>" in `doIt<Bar<*>>()`
      is KtTypeProjection ->
          match<KtTypeProjection>().apply {
            addChildMatcher(
                { it.typeReference },
                parseKotlinRecursive(checkNotNull(node.typeReference)),
                inheritShouldMatchNull = true)
          }
      // for example "Bar" in `val bar: Bar`
      is KtTypeReference ->
          loadIfVariableOr(node.text) {
            match<KtTypeReference>().apply { addChildMatcher { it.text == node.text } }
          }
      else -> error("unsupported: ${node.javaClass}")
    }
        as PsiAstMatcher<T>
  }

  /** Same as [parseKotlinRecursive] but for Java ASTs */
  private fun <T : Any> parseJavaRecursive(node: T): PsiAstMatcher<T> {
    return when (node) {
      // for example: `private final Foo foo = new Foo(5);`
      is PsiField ->
          loadIfVariableOr(node.nameIdentifier.text) {
                match<PsiField>().apply {
                  addChildMatcher { property -> property.name == node.nameIdentifier.text }
                }
              }
              .apply {
                node.initializer?.let {
                  addChildMatcher(transform = { it.initializer }, matcher = parseJavaRecursive(it))
                }
              }
      // for example: `doIt(1, b)`
      is PsiMethodCallExpression ->
          match<PsiMethodCallExpression>().apply {
            addChildMatcher({ it.methodExpression }, parseJavaRecursive(node.methodExpression))
            addMatchersInOrderList(
                { it.argumentList.expressions.toList() },
                node.argumentList.expressions.map { expression -> parseJavaRecursive(expression) })
            addMatchersInOrderList(
                { it.typeArgumentList.typeParameterElements.toList() },
                node.typeArgumentList.typeParameterElements.map { typeElement ->
                  parseJavaRecursive(typeElement)
                })
          }
      // for example `foo.bar`
      is PsiReferenceExpression ->
          node.qualifierExpression?.let { qualifierExpression ->
            match<PsiReferenceExpression>().apply {
              addChildMatcher({ it.qualifierExpression }, parseJavaRecursive(qualifierExpression))
              if (node.referenceNameElement is PsiExpression) {
                addChildMatcher(
                    { it.referenceNameElement }, parseJavaRecursive(node.referenceNameElement!!))
              } else {
                addChildMatcher({
                  it.referenceNameElement?.text == node.referenceNameElement?.text
                })
              }
            }
          }
              ?: run {
                loadIfVariableOr(node.text) {
                  match<PsiExpression>().apply {
                    addChildMatcher { expression -> expression.text == node.text }
                  }
                }
              }
      is PsiClassObjectAccessExpression ->
          match<PsiClassObjectAccessExpression>().apply {
            addChildMatcher({ it.operand }, parseJavaRecursive(node.operand))
          }
      is PsiUnaryExpression -> {
        match<PsiUnaryExpression>().apply {
          addChildMatcher({ it.operand }, parseJavaRecursive(checkNotNull(node.operand)))
          addChildMatcher { it is PsiPostfixExpression == node is PsiPostfixExpression }
          addChildMatcher { it.operationSign.text == node.operationSign.text }
        }
      }
      // any expression for which we don't have more specific handling, such as `1`, or `foo`
      is PsiExpression ->
          loadIfVariableOr(node.text) {
            match<PsiExpression>().apply {
              addChildMatcher { expression -> expression.text == node.text }
            }
          }
      // for example `Bar` in `final Bar bar = new Bar(5);`
      is PsiTypeElement ->
          loadIfVariableOr(node.text) {
            match<PsiTypeElement>().apply {
              addChildMatcher { typeElemenet -> typeElemenet.text == node.text }
            }
          }
      // for example: `@Magic` or `@Foo(1)`
      is PsiAnnotation ->
          loadIfVariableOr(node.nameReferenceElement?.referenceName) {
            match<PsiAnnotation>().apply {
              node.nameReferenceElement?.referenceName?.let { referenceName ->
                addChildMatcher { it.nameReferenceElement?.referenceName == referenceName }
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
      string != null &&
          (string.startsWith("`$") && string.endsWith("$`") ||
              string.startsWith("$") && string.endsWith("$"))

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

    val varName = textContent.removeSurrounding("`$", "$`").removeSurrounding("$")
    val variable = variableNamesToVariables[varName]
    val matcherFromVariable = variable?.matcher
    if (matcherFromVariable == Variable.ANY_SENTINEL) {
      return match<T>().also {
        it.variableName = variable.name
        it.shouldMatchToNull = variable.isOptional
        variable.addConditionsFromVariable(it)
      }
    }
    matcherFromVariable
        ?: error(
            "Template references variable $$varName, " +
                "but one was defined: Add `val $varName by match<...> { ... }` to your template")
    variable.addConditionsFromVariable(matcherFromVariable)
    check(T::class.java == matcherFromVariable.targetType) {
      "Template references variable $$varName as a matcher of ${T::class.java.simpleName}, " +
          "but variable was defined as a matcher of ${matcherFromVariable.targetType.simpleName}"
    }
    return matcherFromVariable as PsiAstMatcher<T>
  }
}
