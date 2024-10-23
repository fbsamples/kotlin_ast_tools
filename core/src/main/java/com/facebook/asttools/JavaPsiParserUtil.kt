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

package com.facebook.asttools

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiImportStatementBase
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiStatement
import com.intellij.testFramework.LightVirtualFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

/**
 * Helper methods to easily generate Java AST node objects from a String. Analogous to
 * `KotlinParserUtil`. Also similar to `JavaParserUtil` except that these methods are based on
 * JetBrains' PSI library (which handles Kotlin interop), whereas `JavaParserUtil` uses GitHub's
 * JavaParser library.
 */
object JavaPsiParserUtil {

  @JvmStatic
  fun parseAsFile(@Language("java") code: String, path: String = "temp.java"): PsiJavaFile {
    val file = LightVirtualFile(path, JavaFileType.INSTANCE, code)
    return PsiManager.getInstance(ProjectHelper.getProject()).findFile(file) as PsiJavaFile
  }

  fun parseAsClassOrInterface(code: String): PsiClass {
    return extract(parseAsFile(code), code)
  }

  fun parseAsMethod(code: String): PsiMethod {
    return extract(
        parseAsFile(
            """
        class Dummy {
          $code
        }
        """
                .trimIndent()),
        code)
  }

  fun parseAsStatement(code: String): PsiStatement {
    return extract(
        parseAsFile(
            """
        class Dummy {
          public void doDummyStuff() {
            $code
          }
        }
        """
                .trimIndent()),
        code)
  }

  fun parseAsExpression(code: String): PsiExpression {
    return extract(
        parseAsFile(
            """
        class Dummy {
          public void doDummyStuff() {
            Something s = $code;
          }
        }
        """
                .trimIndent()),
        code)
  }

  fun parseAsField(code: String): PsiField {
    val fixedCode = if (code.endsWith(";")) code else "$code;"
    return extract(
        parseAsFile(
            """
        class Dummy {
          $fixedCode
        }
        """
                .trimIndent()),
        fixedCode)
  }

  fun parseAsAnnotation(code: String): PsiAnnotation {
    return extract(
        parseAsFile(
            """
        $code
        class Dummy {}
        """
                .trimIndent()),
        code)
  }

  fun parseAsImportStatement(code: String): PsiImportStatementBase {
    return extract(
        parseAsFile(
            """
        $code

        class Dummy {}
        """
                .trimIndent()),
        code)
  }

  private inline fun <reified T : PsiElement> extract(psiJavaFile: PsiJavaFile, code: String): T {
    return psiJavaFile
        .takeIf { it.findDescendantOfType<PsiErrorElement>() == null }
        ?.findDescendantOfType { it.text == code }
        ?: throwParseError(T::class.simpleName.toString(), code)
  }

  private fun throwParseError(type: String, code: String): Nothing {
    error("Cannot parse as $type: '$code'")
  }
}
