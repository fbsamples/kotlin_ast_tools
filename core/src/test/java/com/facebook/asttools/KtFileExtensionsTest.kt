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

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.Test

/** Tests [replaceAll] */
class KtFileExtensionsTest {

  @Test
  fun `replace an integer constant with +1 using a function`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |fun f() {
          |  doIt(1)
          |  doIt(2)
          |  val a = 1
          |}
        """
                .trimMargin()
        )

    val newKtFile =
        ktFile.replaceAll<KtExpression>(
            matcher = { it.text?.toIntOrNull() != null },
            replaceWith = { (it.text.toInt() + 1).toString() },
        )

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun f() {
          |  doIt(2)
          |  doIt(3)
          |  val a = 2
          |}
        """
                .trimMargin()
        )
  }

  @Test
  fun `replace an integer constant with +1 using a list`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |fun f() {
          |  doIt(1)
          |  doIt(2)
          |  val a = 1
          |}
        """
                .trimMargin()
        )

    val nodes = ktFile.collectDescendantsOfType<KtExpression> { it.text?.toIntOrNull() != null }
    val replacements = nodes.map { (it.text.toInt() + 1).toString() }
    val newKtFile = ktFile.replaceAll(nodes, replacements)

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun f() {
          |  doIt(2)
          |  doIt(3)
          |  val a = 2
          |}
        """
                .trimMargin()
        )
  }

  @Test
  fun `remove an only parameter`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |fun f(foo: Foo) {}
        """
                .trimMargin()
        )

    val newKtFile = ktFile.removeAll<KtParameter> { true }

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun f() {}
        """
                .trimMargin()
        )
  }

  @Test
  fun `remove a second parameter`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |fun f(foo: Foo, bar: Bar) {}
        """
                .trimMargin()
        )

    val newKtFile = ktFile.removeAll<KtParameter> { it.name == "bar" }

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun f(foo: Foo) {}
        """
                .trimMargin()
        )
  }

  @Test
  fun `remove only parameter with trailing comma parameter`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |fun f(
          |  foo: Foo,
          |) {}
        """
                .trimMargin()
        )

    val newKtFile = ktFile.removeAll<KtParameter> { true }

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun f() {}
        """
                .trimMargin()
        )
  }

  @Test
  fun `remove last parameter with trailing comma parameter`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |fun f(
          |  foo: Foo,
          |  bar: Bar,
          |) {}
        """
                .trimMargin()
        )

    val newKtFile = ktFile.removeAll<KtParameter> { it.name == "bar" }

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun f(
          |  foo: Foo,
          |) {}
        """
                .trimMargin()
        )
  }

  @Test
  fun `remove a first parameter`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |fun f(foo: Foo, bar: Bar) {}
        """
                .trimMargin()
        )

    val newKtFile = ktFile.removeAll<KtParameter> { it.name == "foo" }

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun f(bar: Bar) {}
        """
                .trimMargin()
        )
  }
}
