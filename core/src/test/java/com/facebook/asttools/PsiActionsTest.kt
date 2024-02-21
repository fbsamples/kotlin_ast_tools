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
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.Test

/** Tests [PsiActions] */
class PsiActionsTest {

  @Test
  fun `add annotation to commentless, annotationless class`() {
    assertThat(KotlinParserUtil.parseAsClassOrObject("class Foo").withAnnotation("@Dummy").text)
        .isEqualTo("@Dummy class Foo")
    assertThat(
            KotlinParserUtil.parseAsClassOrObject("private class Foo {}")
                .withAnnotation("@Dummy")
                .text)
        .isEqualTo("@Dummy private class Foo {}")
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |private class Foo(val name: String) : SuperFoo() {
          |  const val TAG = "Foo"
          |}
        """
                        .trimMargin())
                .withAnnotation("@Dummy")
                .text)
        .isEqualTo(
            """
          |@Dummy private class Foo(val name: String) : SuperFoo() {
          |  const val TAG = "Foo"
          |}
        """
                .trimMargin())
  }

  @Test
  fun `add annotation to commentless, annotationless method`() {
    assertThat(
            KotlinParserUtil.parseAsFunction(
                    """
          |private fun doFoo(name: String) {
          |  println("hello!")
          |}
        """
                        .trimMargin())
                .withAnnotation("@JvmStatic")
                .text)
        .isEqualTo(
            """
          |@JvmStatic private fun doFoo(name: String) {
          |  println("hello!")
          |}
        """
                .trimMargin())
  }

  @Test
  fun `add annotation to commentless, annotated class`() {
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |@Thing("Foo")
          |class Foo(val name: String) {
          |  @Dummy const val TAG = "Foo"
          |}
        """
                        .trimMargin())
                .withAnnotation("@Dummy")
                .text)
        .isEqualTo(
            """
          |@Dummy @Thing("Foo")
          |class Foo(val name: String) {
          |  @Dummy const val TAG = "Foo"
          |}
        """
                .trimMargin())
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |@Dummy() @Thing("Foo")
          |class Foo(val name: String) {
          |  const val TAG = "Foo"
          |}
        """
                        .trimMargin())
                .withAnnotation("@Dummy")
                .text)
        .isEqualTo(
            """
          |@Dummy() @Thing("Foo")
          |class Foo(val name: String) {
          |  const val TAG = "Foo"
          |}
        """
                .trimMargin())
  }

  @Test
  fun `add annotation to commentless, annotated method`() {
    assertThat(
            KotlinParserUtil.parseAsFunction(
                    """
          |@Thing
          |private fun doFoo(name: String) {
          |  println("hello!")
          |}
        """
                        .trimMargin())
                .withAnnotation("@JvmStatic")
                .text)
        .isEqualTo(
            """
          |@JvmStatic @Thing
          |private fun doFoo(name: String) {
          |  println("hello!")
          |}
        """
                .trimMargin())
  }

  @Test
  fun `add annotation to class with comments`() {
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |// This comment belongs directly above Foo
          |class Foo(val name: String) {
          |  /**
          |   * This is a decoy comment! Don't include me when determining insertion point!
          |   */
          |  const val TAG = "Foo"
          |}
        """
                        .trimMargin())
                .withAnnotation("@Dummy")
                .text)
        .isEqualTo(
            """
          |// This comment belongs directly above Foo
          |@Dummy class Foo(val name: String) {
          |  /**
          |   * This is a decoy comment! Don't include me when determining insertion point!
          |   */
          |  const val TAG = "Foo"
          |}
        """
                .trimMargin())

    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |/**
          | * This doc comment is very, very, very, very, very, very, very, very, very, very, very, very,
          | * long for the sake of testing
          | */
          |class Foo(val name: String) {
          |  const val TAG = "Foo"
          |}
        """
                        .trimMargin())
                .withAnnotation("@Dummy")
                .text)
        .isEqualTo(
            """
          |/**
          | * This doc comment is very, very, very, very, very, very, very, very, very, very, very, very,
          | * long for the sake of testing
          | */
          |@Dummy class Foo(val name: String) {
          |  const val TAG = "Foo"
          |}
        """
                .trimMargin())
  }

  @Test
  fun `add supertype to classes with no existing supertypes`() {
    assertThat(KotlinParserUtil.parseAsClassOrObject("class Foo").withSupertype("SuperFoo").text)
        .isEqualTo("class Foo : SuperFoo")
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    "class Foo internal constructor(val kInjector: KInjector)")
                .withSupertype("SuperFoo")
                .text)
        .isEqualTo("class Foo internal constructor(val kInjector: KInjector) : SuperFoo")
    assertThat(
            KotlinParserUtil.parseAsClassOrObject("interface Foo<K, T>")
                .withSupertype("SuperFoo")
                .text)
        .isEqualTo("interface Foo<K, T> : SuperFoo")
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |/**
          | * This doc comment is very, very, very, very, very, very, very, very, very, very, very, very,
          | * long && <[{weird!!}]>!= for the sake of testing
          | */
          |@Dummy class Foo(val name: String) {
          |  const val TAG = "Foo"
          |}
        """
                        .trimMargin())
                .withSupertype("SuperFoo")
                .text)
        .isEqualTo(
            """
          |/**
          | * This doc comment is very, very, very, very, very, very, very, very, very, very, very, very,
          | * long && <[{weird!!}]>!= for the sake of testing
          | */
          |@Dummy class Foo(val name: String) : SuperFoo {
          |  const val TAG = "Foo"
          |}
        """
                .trimMargin())
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |class Foo
          |private constructor (
          |    @JvmField val x: Int,
          |    @JvmField val y: Int,
          |    override val width: Int
          |) {
          |  fun doSomething() = Unit
          |}
        """
                        .trimMargin())
                .withSupertype("SuperFoo")
                .text)
        .isEqualTo(
            """
          |class Foo
          |private constructor (
          |    @JvmField val x: Int,
          |    @JvmField val y: Int,
          |    override val width: Int
          |) : SuperFoo {
          |  fun doSomething() = Unit
          |}
        """
                .trimMargin())
  }

  @Test
  fun `add supertype to classes with existing supertypes`() {
    assertThat(
            KotlinParserUtil.parseAsClassOrObject("private class Foo : Dummy")
                .withSupertype("SuperFoo")
                .text)
        .isEqualTo("private class Foo : Dummy, SuperFoo")
    assertThat(
            KotlinParserUtil.parseAsClassOrObject("private class Foo : Dummy {}")
                .withSupertype("SuperFoo")
                .text)
        .isEqualTo("private class Foo : Dummy, SuperFoo {}")
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    "private class Foo(val name: String) : Dummy(name)")
                .withSupertype("SuperFoo")
                .text)
        .isEqualTo("private class Foo(val name: String) : Dummy(name), SuperFoo")
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |class Foo
          |private constructor (
          |    @JvmField val x: Int,
          |    @JvmField val y: Int,
          |    override val width: Int
          |) : Shape(x, y) {
          |  fun doSomething() = Unit
          |}
        """
                        .trimMargin())
                .withSupertype("SuperFoo")
                .text)
        .isEqualTo(
            """
          |class Foo
          |private constructor (
          |    @JvmField val x: Int,
          |    @JvmField val y: Int,
          |    override val width: Int
          |) : Shape(x, y), SuperFoo {
          |  fun doSomething() = Unit
          |}
        """
                .trimMargin())
  }

  @Test
  fun `don't add supertype when supertype with same name already exists`() {
    var originalText = "private class Foo(val name: String) : SuperFoo()"
    assertThat(KotlinParserUtil.parseAsClassOrObject(originalText).withSupertype("SuperFoo()").text)
        .isEqualTo(originalText)
    originalText = "private class Foo(val name: String) : SuperFoo(name,  10)"
    assertThat(
        KotlinParserUtil.parseAsClassOrObject(originalText)
            .withSupertype("SuperFoo(name, 10)")
            .text)
    originalText = "private class Foo(val name: String) : SuperFoo(name,  10)"
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(originalText)
                .withSupertype("  SuperFoo(name, 10) ")
                .text)
        .isEqualTo(originalText)
    originalText = "private class Foo(val name: String) : SuperFoo(name, 10)"
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(originalText)
                .withSupertype("SuperFoo(name, 3)")
                .text)
        .isEqualTo(originalText)
    originalText = "private class Foo(val name: String) : SuperFoo<String, Foo>"
    assertThat(KotlinParserUtil.parseAsClassOrObject(originalText).withSupertype("SuperFoo").text)
        .isEqualTo(originalText)
  }

  @Test
  fun `add method to classes with existing bodies`() {
    assertThat(
            KotlinParserUtil.parseAsClassOrObject("private class Foo : Dummy {}")
                .withFunction("fun doSomething() = Unit")
                .text)
        .isEqualTo(
            """
          |private class Foo : Dummy {
          |  fun doSomething() = Unit
          |}
        """
                .trimMargin())
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |private class Foo : Dummy {
          |  fun doSomething() = Unit
          |}
        """
                        .trimMargin())
                .withFunction("private fun getFooName(): String = \"Foo\"")
                .text)
        .isEqualTo(
            """
          |private class Foo : Dummy {
          |  fun doSomething() = Unit
          |
          |  private fun getFooName(): String = "Foo"
          |}
        """
                .trimMargin())
    assertThat(
            KotlinParserUtil.parseAsClassOrObject(
                    """
          |class Foo
          |private constructor (
          |    @JvmField val x: Int,
          |    @JvmField val y: Int,
          |    override val width: Int
          |) : Shape(x, y) {
          |  val size = (y - x) * width
          |
          |  fun doSomething() = Unit
          |}
        """
                        .trimMargin())
                .withFunction("private fun getFooName(): String = \"Foo\"")
                .text)
        .isEqualTo(
            """
          |class Foo
          |private constructor (
          |    @JvmField val x: Int,
          |    @JvmField val y: Int,
          |    override val width: Int
          |) : Shape(x, y) {
          |  val size = (y - x) * width
          |
          |  fun doSomething() = Unit
          |
          |  private fun getFooName(): String = "Foo"
          |}
        """
                .trimMargin())
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
                .trimMargin())

    val nodes = ktFile.collectDescendantsOfType<KtExpression> { it.text?.toIntOrNull() != null }
    val replacements = nodes.map { (it.text.toInt() + 1).toString() }
    val newCode = replaceElements(ktFile.text, nodes, replacements)

    assertThat(newCode)
        .isEqualTo(
            """
          |fun f() {
          |  doIt(2)
          |  doIt(3)
          |  val a = 2
          |}
        """
                .trimMargin())
  }
}
