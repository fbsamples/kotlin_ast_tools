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
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtExpression
import org.junit.Test

/** Tests [PsiAstTemplateParser] */
class PsiAstTemplateParserTest {

  @Test
  fun `create matcher with resolver and resolve on type`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |class Foo {
          |  val bar = foo.get().get()
          |  var bar2 = foo.getMore().get()
          |  var bar3 = foo.getEvenMore().get()
          |}
        """
                .trimMargin())

    val parser =
        PsiAstTemplateParser(
            object : Resolver {
              override fun resolveToFullyQualifiedType(psiElement: PsiElement): String? =
                  when (psiElement.text) {
                    "foo.get()" -> "com.facebook.bar.Bar"
                    "foo.getMore()" -> "com.facebook.bar.NotBar"
                    else -> null
                  }

              override fun resolveToFullyQualifiedTypeAndSupertypes(
                  psiElement: PsiElement
              ): List<String>? =
                  when (psiElement.text) {
                    "foo.get()" -> listOf("com.facebook.bar.Bar", "com.facebook.bar.Baz")
                    "foo.getMore()" -> listOf("com.facebook.bar.NotBar", "com.facebook.bar.NotBaz")
                    else -> null
                  }
            })

    assertThat(
            parser
                .parseTemplateWithVariables<KtExpression>("#a{type=com.facebook.bar.Bar}#.get()")
                .findAll(ktFile)
                .map { it.text })
        .containsExactly("foo.get().get()")

    assertThat(
            parser
                .parseTemplateWithVariables<KtExpression>("#a{type=com.facebook.bar.Baz}#.get()")
                .findAll(ktFile)
                .map { it.text })
        .containsExactly("foo.get().get()")
  }
}
