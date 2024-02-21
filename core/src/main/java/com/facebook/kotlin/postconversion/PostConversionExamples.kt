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

package com.facebook.kotlin.postconversion

import com.facebook.kotlin.asttools.KotlinParserUtil
import com.facebook.kotlin.asttools.replaceAll
import com.facebook.kotlin.matching.replaceAllExpressions
import com.intellij.psi.PsiElement
import java.io.File
import java.io.PrintStream
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/** Updates a Kotlin file which was just converted from Java to better match expcetations */
object PostConversionExamples {
  @JvmStatic
  fun main(args: Array<String>) {
    main(System.out, args)
  }

  fun main(out: PrintStream, args: Array<String>) {
    if (args.isEmpty()) {
      out.println("no input files")
    }
    val steps = listOf(::replaceTextUtils, ::replaceGuavaStrings, ::replaceVarsWithVals)
    for (arg in args) {
      val file = File(arg)
      var ktFile =
          try {
            KotlinParserUtil.parseAsFile(file.readText())
          } catch (e: Exception) {
            System.err.println("Error reading file ${e.message}")
            continue
          }
      for (step in steps) {
        ktFile = step(ktFile)
      }
      file.writeText(replaceTextUtils(ktFile).text)
    }
  }

  /** Replaces usages of Android TextUtils class with built in Kotlin methods */
  fun replaceTextUtils(ktFile: KtFile): KtFile {
    if ("android.text.TextUtils" !in ktFile.text) {
      return ktFile
    }
    return ktFile
        .replaceAllExpressions(
            "TextUtils.equals(#a#, #b#)",
            replaceWith = { result, _ ->
              val needsParenthesis =
                  result.parent is KtBinaryExpression || result.parent is KtQualifiedExpression
              if (needsParenthesis) "(#a# == #b#)" else "#a# == #b#"
            })
        .replaceAllExpressions("TextUtils.isEmpty(#a#)", "#a#.isNullOrEmpty()")
  }

  fun replaceGuavaStrings(ktFile: KtFile): KtFile {
    if ("com.google.common.base.Strings" !in ktFile.text) {
      return ktFile
    }

    return ktFile
        .replaceAllExpressions("Strings.isNullOrEmpty(#a#)", "#a#.isNullOrEmpty()")
        .replaceAllExpressions("Strings.nullToEmpty(#a#)", "#a#.orEmpty()")
  }

  /**
   * Tries to simplify some simple cases where a `val` can replace a `var`
   *
   * Right now it can only handle the simplest of cases:
   * ```
   * var myVar = value1
   * if (something) {
   *   myVar = value2
   * }
   * ```
   *
   * This will be transformed into:
   * ```
   * val myVar = if (something) value2 else value1
   * ```
   */
  fun replaceVarsWithVals(ktFile: KtFile): KtFile {
    val nodes = mutableListOf<PsiElement>()
    val replacements = mutableListOf<String>()
    ktFile.accept(
        object : KtTreeVisitorVoid() {
          override fun visitProperty(property: KtProperty) {
            super.visitProperty(property)
            val parent = property.parent
            val initializer = property.initializer
            if (!property.isVar || initializer == null) {
              return
            }

            if (parent !is KtBlockExpression) {
              return
            }

            val nextStatement: KtExpression =
                parent.statements.getOrNull(parent.statements.indexOf(property) + 1) ?: return

            if (!(nextStatement is KtIfExpression && nextStatement.`else` == null)) {
              return
            }

            val then = nextStatement.then
            if (then !is KtBlockExpression || then.statements.size != 1) {
              return
            }

            val thenStatement = then.statements.first()
            if (thenStatement !is KtBinaryExpression ||
                thenStatement.operationReference.text != "=" ||
                thenStatement.left?.text != property.name) {
              return
            }

            // If the variable is already being referenced in the if we can't transform
            if (nextStatement
                .collectDescendantsOfType<KtExpression> { it.text == property.name }
                .size > 1) {
              return
            }

            if (countAssignmentsInNode(parent, property) > 1) {
              return
            }

            val right = thenStatement.right ?: return
            val condition = nextStatement.condition ?: return
            val updatedProperty =
                property.text
                    .replaceFirst("var", "val")
                    .replace(
                        initializer.text,
                        "if (${condition.text}) ${right.text} else ${initializer.text}")
            val firstInitializerInstance = updatedProperty.indexOf(initializer.text)
            if (firstInitializerInstance >= 0 &&
                updatedProperty.indexOf(initializer.text, firstInitializerInstance + 1) >= 0) {
              return
            }
            nodes.add(property)
            replacements.add(updatedProperty)
            nodes.add(nextStatement)
            replacements.add("")
          }
        })
    if (nodes.isEmpty()) {
      return ktFile
    } else {
      return ktFile.replaceAll(nodes, replacements)
    }
  }

  private fun countAssignmentsInNode(parent: PsiElement, property: KtElement): Int {
    var count = 0
    parent.accept(
        object : KtTreeVisitorVoid() {
          override fun visitBinaryExpression(expression: KtBinaryExpression) {
            super.visitBinaryExpression(expression)
            if (expression.asAssignment() != null && expression.left?.text == property.name) {
              count++
            }
          }
        })
    return count
  }
}
