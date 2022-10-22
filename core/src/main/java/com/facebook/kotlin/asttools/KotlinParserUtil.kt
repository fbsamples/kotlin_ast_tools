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

package com.facebook.kotlin.asttools

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

/** Helper methods to easily generate Kotlin AST node objects from a String */
object KotlinParserUtil {

  private val kotlinCoreEnvironment: KotlinCoreEnvironment by lazy {
    val disposable = Disposer.newDisposable()
    KotlinCoreEnvironment.createForProduction(
        disposable, getConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES)
  }

  fun parseAsFile(code: String): KtFile {
    val file = LightVirtualFile("temp.kt", KotlinFileType.INSTANCE, code)
    return PsiManager.getInstance(kotlinCoreEnvironment.project).findFile(file) as KtFile
  }

  fun parseAsClassOrObject(code: String): KtClassOrObject {
    return extract(parseAsFile(code), code)
  }

  fun parseAsImportStatement(code: String): KtImportDirective {
    return parseAsFile(code).importDirectives.first()
  }

  fun parseAsProperty(code: String): KtProperty {
    return extract(
        parseAsFile(
            """
      class `DUMMY NAME` {
        $code
      }
    """
                .trimIndent()),
        code)
  }

  fun parseAsSupertype(code: String): KtSuperTypeListEntry {
    val trimmedCode = code.trim()
    return extract(
        parseAsFile(
            """
      class `DUMMY NAME` : $trimmedCode {}
    """
                .trimIndent()),
        trimmedCode)
  }

  fun parseAsFunction(code: String): KtNamedFunction {
    return extract(parseAsFile(code), code)
  }

  fun parseAsDeclaration(code: String): KtDeclaration {
    return extract(parseAsFile("val `DUMMY NAME`: Any\n" + code), code)
  }

  fun parseAsExpression(code: String): KtExpression {
    return extract(
        parseAsFile(
            """
      val `DUMMY NAME` = $code
    """
                .trimIndent()),
        code)
  }

  fun parseAsAnnotationEntry(code: String): KtAnnotationEntry {
    return extract(
        parseAsFile(
            """
       $code val `DUMMY NAME` = null
    """
                .trimIndent()),
        code)
  }

  fun parseAsParameter(code: String): KtParameter {
    return extract(
        parseAsFile(
            """
       fun `DUMMY NAME`($code) = TODO()
    """
                .trimIndent()),
        code)
  }

  private inline fun <reified T : PsiElement> extract(ktFile: KtFile, code: String): T {
    return ktFile
        .takeIf { it.findDescendantOfType<PsiErrorElement>() == null }
        ?.findDescendantOfType { it.text == code }
        ?: throwParseError(T::class.simpleName?.removePrefix("Kt")?.toLowerCase().toString(), code)
  }

  private fun throwParseError(type: String, code: String): Nothing {
    error("Cannot parse as $type: '$code'")
  }

  private fun getConfiguration(): CompilerConfiguration {
    val configuration = CompilerConfiguration()
    configuration.put(
        CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false))
    return configuration
  }
}
