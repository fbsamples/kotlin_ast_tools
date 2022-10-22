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

package com.facebook.kotlin.matching

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.junit.Test

/** Tests [KtAstTemplate] */
class KtAstTemplateKtTest {

  @Test
  fun `when parsing from template, match on properties`() {
    val ktFile =
        load(
            """
          |class Foo {
          |  val bar: Bar by SuperDelegate
          |  val bar2: Bar
          |  var bar3: Bar by SuperDelegate
          |  val barString = "Bar".uppercase()
          |}
        """
                .trimMargin())

    val delegatedPropertyResults: List<KtProperty> =
        ktFile.findAll(template { "val $any: $any by SuperDelegate" })
    val initializedPropertyResults: List<KtProperty> =
        ktFile.findAll(template { "val $any = $any.uppercase()" })

    assertThat(delegatedPropertyResults).hasSize(2)
    assertThat(delegatedPropertyResults[0].text).isEqualTo("val bar: Bar by SuperDelegate")
    assertThat(initializedPropertyResults).hasSize(1)
    assertThat(initializedPropertyResults[0].text).isEqualTo("val barString = \"Bar\".uppercase()")
  }

  @Test
  fun `when parsing from template, match on expression`() {
    val ktFile =
        load(
            """
          |class Foo {
          |  val bar: Int = doIt(1 + 1)
          |}
        """
                .trimMargin())
    val results: List<KtExpression> = ktFile.findAll(template { "doIt(1 + 1)" })

    assertThat(results).hasSize(1)
  }

  @Test
  fun `match template for annotation entry`() {
    val ktFile =
        load(
            """
          |fun foo() {
          |  @Magic val a = 5
          |  @NotMagic val b = 5
          |}
        """
                .trimMargin())
    val results: List<KtAnnotationEntry> = ktFile.findAll(template { "@Magic" })

    assertThat(results).hasSize(1)
    assertThat(results[0].text).isEqualTo("@Magic")
  }

  @Test
  fun `match template for function call`() {
    val ktFile =
        load(
            """
          |fun foo(b: String): Int {
          |  val a = doIt(1, name = b) // yes
          |  val a = doIt(2, name = b) // no
          |  return a
          |}
        """
                .trimMargin())
    val results: List<KtExpression> = ktFile.findAll(template { "doIt(1, name = b)" })

    assertThat(results.map { it.text }).containsExactly("doIt(1, name = b)")
  }

  @Test
  fun `match template for function call with variables`() {
    val ktFile =
        load(
            """
          |fun foo() {
          |  doIt(1 + 1) // yes
          |  doIt(1) // no
          |}
        """
                .trimMargin())

    val results: List<KtExpression> =
        ktFile.findAll(
            template {
              val a by match<KtExpression> { expression -> expression.text == "1 + 1" }
              "doIt($a)"
            })

    assertThat(results.map { it.text }).containsExactly("doIt(1 + 1)")
  }

  @Test
  fun `replace using template and variables`() {
    val ktFile =
        load(
            """
          |fun foo() {
          |  doIt(1, 2)
          |}
        """
                .trimMargin())

    val newKtFile =
        ktFile.replaceAllWithVariables(
            template<KtExpression> {
              val a by match<KtExpression>()
              val b by match<KtExpression>()
              "doIt($a, $b)"
            },
            replaceWith = { (_, variables) ->
              val a by variables
              val b by variables
              "doIt($b, $a)"
            })

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun foo() {
          |  doIt(2, 1)
          |}
        """
                .trimMargin())
  }

  @Test
  fun `match template for qualified calls`() {
    val ktFile =
        load(
            """
          |fun foo() {
          |  a?.b() // yes
          |  a?.c() // no
          |  a.b() // no
          |}
        """
                .trimMargin())

    val results: List<KtExpression> = ktFile.findAll(template { "a?.b()" })

    assertThat(results.map { it.text }).containsExactly("a?.b()")
  }

  @Test
  fun `match template for a class expressions`() {
    val ktFile =
        load(
            """
          |fun foo() {
          |  println(Bar::class)
          |  println(Bar
          |      ::
          |      class)
          |}
        """
                .trimMargin())

    val results: List<KtExpression> = ktFile.findAll(template { "Bar::class" })

    assertThat(results.map { it.text })
        .containsExactly("Bar::class", "Bar\n      ::\n" + "      class")
  }

  @Test
  fun `do not match on call expression with receiver`() {
    val ktFile =
        load(
            """
          |fun foo(bar: Bar) {
          |  doIt(1)
          |  bar.doIt(2)
          |  doIt(3).again()
          |}
        """
                .trimMargin())
    val results: List<KtExpression> = ktFile.findAll(template { "doIt($any)" })

    assertThat(results.map { it.text }).containsExactly("doIt(1)", "doIt(3)")
  }

  @Test
  fun `match with unsafe dereference`() {
    val ktFile =
        load(
            """
          |fun foo(bar: Bar) {
          |  doIt(1)!!
          |  doIt(2)
          |  doIt(doIt(3)!!)
          |}
        """
                .trimMargin())
    val results: List<KtExpression> = ktFile.findAll(template { "doIt($any)!!" })

    assertThat(results.map { it.text }).containsExactly("doIt(1)!!", "doIt(3)!!")
  }

  @Test
  fun `match with prefix and postfix unary expressions`() {
    val ktFile =
        load(
            """
          |fun foo(i: Int) {
          |  i++
          |  ++i
          |}
        """
                .trimMargin())
    val resultsPrefix: List<KtExpression> = ktFile.findAll(template { "++$any" })
    val resultsPostfix: List<KtExpression> = ktFile.findAll(template { "$any++" })

    assertThat(resultsPrefix.map { it.text }).containsExactly("++i")
    assertThat(resultsPostfix.map { it.text }).containsExactly("i++")
  }
}
