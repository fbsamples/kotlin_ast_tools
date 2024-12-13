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

package com.facebook.asttools.analysis

import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiCodeBlock
import org.jetbrains.kotlin.com.intellij.psi.PsiDeclarationStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiForStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiForeachStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.com.intellij.psi.PsiLambdaExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.com.intellij.psi.PsiWhileStatement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parents

object DeclarationsFinder {

  sealed interface Declaration {
    /**
     * The most recent declaration for a callable of a given name. Same as `last` for `Simple`
     * declarations.
     */
    val value: PsiElement

    /**
     * The declaration last visited as we work our way up the callstack looking for declarations of
     * a given name. For example, given a class Foo like
     *
     * ```
     * class Foo(val name: String) {
     *   fun doThing(name: String) = println(name)
     * }
     * ```
     *
     * `last` would return `val name: String` for `name`.
     */
    val _last: PsiElement

    val allValues: Array<PsiElement>
  }

  class Simple(override val value: PsiElement) : Declaration {
    override val _last = value
    override val allValues: Array<PsiElement>
      get() = arrayOf(value)

    override fun toString(): String {
      return "Simple(${value.text})"
    }
  }

  class Overloaded(val values: Array<PsiElement>) : Declaration {
    override val value = values.first()
    override val _last = values.last()
    override val allValues: Array<PsiElement>
      get() = values

    override fun toString(): String {
      return "Overloaded(${values.joinToString(", ") { it.text }})"
    }
  }

  /**
   * Finds the closest local declaration available at the given [PsiElement] with the given name.
   * For example, given a class Foo like
   *
   * ```
   * class Foo(val name: String) {
   *   fun doThing(name: String) = println(name)
   * }
   * ```
   *
   * calling `getDeclarationAt` for the PsiElement representing `println(name)` and the name
   * `"name"` would return the PsiParameter node for parameter `name: String` in function `doThing`
   *
   * Possible declarations it finds are classes known at the target, named functions, class
   * properties and local variables
   */
  fun getDeclarationAt(psiElement: PsiElement, name: String): PsiElement? =
      getDeclarationsAt(psiElement)[name]?.value

  /** Same as [getDeclarationAt], but ignores function declarations */
  fun getVariableDeclarationAt(psiElement: PsiElement, name: String): PsiElement? {
    return when (val currentDeclaration = getDeclarationsAt(psiElement)[name]) {
      is Simple -> currentDeclaration.value.takeIf { isVariableOrConstructorParameter(it) }
      is Overloaded ->
          currentDeclaration.values.firstOrNull { isVariableOrConstructorParameter(it) }
      else -> null
    }
  }

  private fun isVariableOrConstructorParameter(element: PsiElement): Boolean {
    return element is PsiVariable ||
        element is KtProperty ||
        (element is KtParameter && element.valOrVarKeyword != null)
  }

  /**
   * Finds all known local declarations available the given [PsiElement] and returns a map where the
   * keys are the declarations names, and the value is an object with a reference to the
   * [PsiElement] in which they are declared
   *
   * Possible declarations it find are classes known at the target, named functions, class
   * properties and local variables
   */
  fun getDeclarationsAt(psiElement: PsiElement): Map<String, Declaration> {
    val result = mutableMapOf<String, Declaration>()

    fun handle(psiElement: PsiElement) {
      fun add(psiElement: PsiElement) {
        val name: String =
            (psiElement as? PsiNamedElement)?.name
                ?: (psiElement as? KtLambdaExpression)?.let {
                  if (it.valueParameters.isEmpty()) "it" else null
                }
                ?: return
        val previousDeclaration = result[name]
        if (previousDeclaration?._last == psiElement) {
          return
        }
        result[name] =
            when (previousDeclaration) {
              null -> Simple(psiElement)
              is Simple -> Overloaded(arrayOf(previousDeclaration.value, psiElement))
              is Overloaded -> Overloaded(previousDeclaration.values + psiElement)
            }
      }

      when (psiElement) {
        // Kotlin
        is KtProperty -> add(psiElement)
        is KtFunction -> add(psiElement)
        is KtParameter -> add(psiElement)
        is KtClassOrObject -> {
          for (declaration in psiElement.primaryConstructorParameters) {
            if (declaration.valOrVarKeyword != null) {
              handle(declaration)
            }
          }
          for (declaration in psiElement.declarations) {
            if (declaration !is KtClass) {
              handle(declaration)
            }
          }
          add(psiElement)
        }
        is KtLambdaExpression -> {
          if (psiElement.valueParameters.isNotEmpty()) {
            for (valueParameter in psiElement.valueParameters) {
              add(valueParameter)
            }
          } else {
            add(psiElement)
          }
        }
        is KtFile -> {
          for (declaration in psiElement.declarations) {
            handle(declaration)
          }
        }
        is KtPropertyAccessor -> {
          if (psiElement.valueParameters.isNotEmpty()) {
            for (valueParameter in psiElement.valueParameters) {
              add(valueParameter)
            }
          }
        }
        // Java
        is PsiMethod -> {
          add(psiElement)
        }
        is PsiField -> {
          add(psiElement)
        }
        is PsiClass -> {
          psiElement.fields.forEach { field -> add(field) }
          psiElement.methods.forEach { method -> add(method) }
          if (psiElement.constructors.isNotEmpty()) {
            psiElement.constructors.forEach { constructor -> add(constructor) }
          } else {
            add(psiElement)
          }
          psiElement.innerClasses.forEach { innerClass -> add(innerClass) }
        }
        is PsiVariable -> {
          add(psiElement)
        }
        is PsiJavaFile -> {}
      }
    }

    // iterate over all parents of the given node, for each node which can introduce declarations
    // we delegate the nodes we need to recursively figure out to [handle]
    val previousNodes = mutableListOf(psiElement)
    for (parent in psiElement.parents) {
      when (parent) {
        // Kotlin
        is KtClassOrObject -> {
          if (previousNodes.any {
            it in parent.getAnonymousInitializers() ||
                it is KtSuperTypeList ||
                (it is KtProperty &&
                    it in parent.declarations &&
                    it.accessors.none { it in previousNodes })
          }) {
            for (constructorParam in parent.primaryConstructorParameters) {
              if (constructorParam.valOrVarKeyword == null) {
                // handle constructor parameters that are only visible to init code
                handle(constructorParam)
              }
            }
          }
          handle(parent)
        }
        is KtFile -> handle(parent)
        is KtFunction -> {
          for (valueParameter in parent.valueParameters) {
            handle(valueParameter)
          }
        }
        is KtLambdaExpression -> {
          handle(parent)
        }
        is KtForExpression -> {
          val loopParameter = parent.loopParameter
          if (loopParameter != null && parent.body.isAncestor(psiElement, strict = false)) {
            handle(loopParameter)
          }
        }
        is KtBlockExpression -> {
          var prevSibling = previousNodes.last().prevSibling
          while (prevSibling != null) {
            if (prevSibling is KtProperty) {
              handle(prevSibling)
            }
            prevSibling = prevSibling.prevSibling
          }
        }
        is KtPropertyAccessor -> {
          if (parent.valueParameters.isNotEmpty()) {
            for (valueParameter in parent.valueParameters) {
              handle(valueParameter)
            }
          }
        }
        // Java
        is PsiMethod -> {
          parent.parameterList.parameters.forEach { parameter -> handle(parameter) }
        }
        is PsiLambdaExpression -> {
          parent.parameterList.parameters.forEach { parameter -> handle(parameter) }
        }
        is PsiCodeBlock -> {
          var prevSibling = previousNodes.last().prevSibling
          while (prevSibling != null) {
            if (prevSibling is PsiDeclarationStatement) {
              prevSibling.declaredElements.forEach { declaredElement -> handle(declaredElement) }
            }
            prevSibling = prevSibling.prevSibling
          }
        }
        is PsiForStatement -> {
          val initialization = parent.initialization
          if (initialization is PsiDeclarationStatement &&
              parent.body.isAncestor(psiElement, strict = false)) {
            initialization.declaredElements.forEach { declaredElement -> handle(declaredElement) }
          }
        }
        is PsiForeachStatement -> {
          val iterationParameter = parent.iterationParameter
          if (parent.body.isAncestor(psiElement, strict = false)) {
            handle(iterationParameter)
          }
        }
        is PsiWhileStatement -> {
          val condition = parent.condition
          if (condition != null) {
            handle(condition)
          }
        }
        is PsiJavaFile -> {
          handle(parent)
        }
        is PsiClass -> {
          handle(parent)
        }
      }
      previousNodes += parent
    }

    return result
  }

  /**
   * Given a reference to a name, and its declaration (which you can get from [getDeclarationsAt])
   * returns a list of all smart cast constraints that apply
   *
   * This is a best case effort, and does not handle all cases
   *
   * Example, if we look for `a`'s usage in `println("2: " + a)` this result will contain the
   * [PsiElement] for `a is String` (but not the other smart cast which does not apply):
   * ```
   * val a: Any = ...
   * if (a is Int) {
   *   println("1:" + a)
   * }
   * if (a is String) {
   *   println("2:" + a)
   * }
   * ```
   */
  fun findSmartCastConstraints(
      simpleNameExpression: KtSimpleNameExpression,
      declaration: KtElement
  ): List<PsiElement> {
    check(declaration is KtNamedDeclaration || declaration is KtLambdaExpression)
    val result = mutableListOf<PsiElement>()
    val declarationParents = declaration.parents
    var previous: PsiElement = simpleNameExpression
    for (parent in simpleNameExpression.parents) {
      if (parent in declarationParents) {
        break
      }

      when (parent) {
        // parent is an if expression, condition is either true or false
        is KtIfExpression -> {
          val condition = parent.condition
          if (condition is KtIsExpression &&
              condition.leftHandSide.text == simpleNameExpression.text &&
              (previous == parent.then && !condition.isNegated ||
                  previous == parent.`else` && condition.isNegated)) {
            result += condition
          }
        }
        // previous statements might return, making some conditions true or false
        is KtBlockExpression -> {
          var prevSibling = previous.prevSibling
          while (prevSibling != null) {
            if (prevSibling is KtIfExpression) {
              val condition = prevSibling.condition
              if (condition is KtIsExpression &&
                  condition.leftHandSide.text == simpleNameExpression.text &&
                  (condition.isNegated && alwaysReturns(prevSibling.then) ||
                      !condition.isNegated && alwaysReturns(prevSibling.`else`))) {
                result += condition
              }
            }
            prevSibling = prevSibling.prevSibling
          }
        }
      }

      // When traversing KtIfExpression this extra node appears, but then does not align with the
      // KtIfExprsssions's `then` field, so we skip it
      if (parent is KtContainerNodeForControlStructureBody) {
        continue
      }
      previous = parent
    }

    return result
  }

  private fun alwaysReturns(expression: KtExpression?): Boolean =
      when (expression) {
        is KtReturnExpression -> true
        is KtBlockExpression -> alwaysReturns(expression.statements.lastOrNull())
        is KtIfExpression -> alwaysReturns(expression.then) && alwaysReturns(expression.`else`)
        is KtForExpression -> alwaysReturns(expression.body)
        is KtWhileExpression -> alwaysReturns(expression.body)
        is KtWhenExpression ->
            expression.elseExpression != null &&
                expression.entries.all { alwaysReturns(it.expression) }
        else -> false
      }
}
