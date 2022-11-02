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

import java.lang.IllegalArgumentException
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.junit.Test

/** Tests [KtAstMatcher] */
class KtAstMatcherTest {

  @Test
  fun `example test showing multiple feature matching a class object`() {
    val ktFile =
        load(
            """
          |class Foo {
          |  fun doThat() = "yes"
          |  fun doIt() = 1
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtClassOrObject>().apply {
              addChildMatcher { it.name?.matches("F.*".toRegex()) == true }
              addChildMatcher { it.getDeclarationKeyword()?.text == "class" }
              addChildMatcher { it.declarations.any { it is KtNamedFunction && it.name == "doIt" } }
              addChildMatcher {
                it.declarations.any {
                  it is KtNamedFunction && it.name?.endsWith("That") == true && !it.isTopLevel
                }
              }
            })

    assertThat(results).hasSize(1)
    assertThat(results[0].name).isEqualTo("Foo")
    assertThat(results[0].getDeclarationKeyword()?.text).isEqualTo("class")
  }

  @Test
  fun `match on properties with delegates`() {
    val ktFile =
        load(
            """
          |class Foo {
          |  val bar: Bar by SuperDelegate
          |  val bar2: Bar
          |  var bar2: Bar by SuperDelegate
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtProperty>().apply {
              addChildMatcher { it.valOrVarKeyword.text == "val" }
              addChildMatcher { it.delegateExpression?.text == "SuperDelegate" }
              addChildMatcher { it.typeReference?.text == "Bar" }
            })

    assertThat(results).hasSize(1)
    assertThat(results[0].delegateExpression?.text).isEqualTo("SuperDelegate")
  }

  @Test
  fun `match on suspend function modifier`() {
    val ktFile =
        load(
            """
          |suspend fun foo(): Int {
          |  return 10
          |}
          |
          |private suspend fun bar(): Int {
          |  return 10
          |}
          |
          |fun bar(): Int {
          |  return 10
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtNamedFunction>().apply {
              addChildMatcher { it.modifierList?.text?.matches(".*suspend.*".toRegex()) == true }
            })

    assertThat(results).hasSize(2)
    assertThat(results[0]).isInstanceOf(KtNamedFunction::class.java)
    assertThat(results[0].name).isEqualTo("foo")
    assertThat(results[1]).isInstanceOf(KtNamedFunction::class.java)
    assertThat(results[1].name).isEqualTo("bar")
  }

  @Test
  fun `match on function annotation`() {
    val ktFile =
        load(
            """
          |@FixMe fun foo(): Int {
          |  return 10
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtNamedFunction>().apply {
              addCustomMatcher { it.annotationEntries.any { it.text == "@FixMe" } }
            })

    assertThat(results).hasSize(1)
    assertThat(results[0]).isInstanceOf(KtNamedFunction::class.java)
    assertThat(results[0].annotationEntries[0].text).isEqualTo("@FixMe")
  }

  @Test
  fun `match on function return type`() {
    val ktFile =
        load(
            """
          |fun foo(): Int = 10
          |fun foo2(): String = "10"
          |fun foo3() = Unit
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtNamedFunction>().apply { addChildMatcher { it.typeReference?.text == "Int" } })

    assertThat(results).hasSize(1)
    assertThat(results[0]).isInstanceOf(KtNamedFunction::class.java)
    assertThat(results[0].typeReference?.text).isEqualTo("Int")
  }

  @Test
  fun `match call expression on indexed value arguments`() {
    val ktFile =
        load(
            """
          |suspend fun foo(): Int {
          |  withContext(Dispatchers.IO, ViewerContext, Env) {
          |     print("hi")
          |  }
          |  return 10
          |}
        """
                .trimMargin())
    val matchResults =
        ktFile.findAll(
            match<KtCallExpression>().apply {
              addChildMatcher { it.referenceExpression()?.text == "withContext" }
              addChildMatcher { it.valueArguments.getOrNull(0)?.text == "Dispatchers.IO" }
              addChildMatcher { it.valueArguments.getOrNull(1)?.text == "ViewerContext" }
              addChildMatcher { it.valueArguments.getOrNull(2)?.text == "Env" }
              addChildMatcher { it.valueArguments.size == 4 }
            })

    assertThat(matchResults).hasSize(1)
    assertThat(matchResults[0]).isInstanceOf(KtCallExpression::class.java)
    assertThat(matchResults[0].referenceExpression()?.text).isEqualTo("withContext")
    assertThat(matchResults[0].valueArgumentList?.children).hasSize(3)

    val noMatchResults =
        ktFile.findAll(
            match<KtCallExpression>().apply {
              addChildMatcher { it.referenceExpression()?.text == "withContext" }
              addChildMatcher { it.valueArguments.getOrNull(3)?.text == "Env" }
            })

    assertThat(noMatchResults).hasSize(0)
  }

  @Test
  fun `match call expression on first value arguments`() {
    val ktFile =
        load(
            """
          |suspend fun foo(): Unit {
          |  withContextFoo(Dispatchers.IO, ViewerContext, Env)
          |}
          |
          |suspend fun bar(): Unit {
          |  withContextBar(Dispatchers.IO, Env)
          |}
          |
          |suspend fun bar2(): Unit {
          |  withContextBar2(Extras, Dispatchers.IO, Env)
          |}
        """
                .trimMargin())
    val results: List<KtCallExpression> =
        ktFile.findAll(
            match<KtCallExpression>().apply {
              addChildMatcher { it.referenceExpression()?.text != null }
              addChildMatcher { it.valueArguments.firstOrNull()?.text == "Dispatchers.IO" }
            })

    assertThat(results).hasSize(2)
    assertThat(results[0]).isInstanceOf(KtCallExpression::class.java)
    assertThat(results[0].referenceExpression()?.text).isEqualTo("withContextFoo")
    assertThat(results[1]).isInstanceOf(KtCallExpression::class.java)
    assertThat(results[1].referenceExpression()?.text).isEqualTo("withContextBar")
  }

  @Test
  fun `match call expression on last value arguments`() {
    val ktFile =
        load(
            """
          |suspend fun foo(): Unit {
          |  withContextFoo(Dispatchers.IO, ViewerContext, Env)
          |  return 10
          |}
          |
          |suspend fun bar(): Unit {
          |  withContextBar(Dispatchers.IO, Env)
          |  return 10
          |}
          |suspend fun bar2(): Unit {
          |  withContextBar2(Dispatchers.IO, Env, Extras)
          |  return 10
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtCallExpression>().apply {
              addChildMatcher { it.referenceExpression()?.text != null }
              addChildMatcher { it.valueArguments.lastOrNull()?.text == "Env" }
            })

    assertThat(results).hasSize(2)
    assertThat(results[0]).isInstanceOf(KtCallExpression::class.java)
    assertThat(results[0].referenceExpression()?.text).isEqualTo("withContextFoo")
    assertThat(results[1]).isInstanceOf(KtCallExpression::class.java)
    assertThat(results[1].referenceExpression()?.text).isEqualTo("withContextBar")
  }

  @Test
  fun `match call expression on any value arguments`() {
    val ktFile =
        load(
            """
          |suspend fun foo(): Unit {
          |  withContextFoo(Dispatchers.IO, ViewerContext, Env)
          |}
          |
          |suspend fun bar(): Unit {
          |  withContextBar(Dispatchers.IO, Env)
          |}
          |
          |suspend fun bar2(): Unit {
          |  withContextBar2(Dispatchers.IO)
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtCallExpression>().apply {
              addChildMatcher { it.valueArguments.any { it.text == "Env" } }
            })

    assertThat(results).hasSize(2)
    assertThat(results[0]).isInstanceOf(KtCallExpression::class.java)
    assertThat(results[0].referenceExpression()?.text).isEqualTo("withContextFoo")
    assertThat(results[1]).isInstanceOf(KtCallExpression::class.java)
    assertThat(results[1].referenceExpression()?.text).isEqualTo("withContextBar")
  }

  @Test
  fun `match on modifiers for properties`() {
    val ktFile =
        load(
            """
          |private val foo = 5
        """
                .trimMargin())
    val results: List<KtProperty> =
        ktFile.findAll(
            match<KtProperty>().apply {
              addChildMatcher { it.modifierList?.text?.matches("private.*".toRegex()) == true }
            })

    assertThat(results.map { it.text }).containsExactly("private val foo = 5")
  }

  @Test
  fun `match on owners`() {
    val ktFile =
        load(
            """
          |object Foo {
          |  private val foo = 5
          |}
          |
          |object Bar {
          |  private val bar = 5
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll<KtProperty>(
            match<KtProperty>().apply {
              addChildMatcher { it.parents.any { it is KtClassOrObject && it.name == "Foo" } }
            })

    assertThat(results).hasSize(1)
    assertThat(results[0].text).isEqualTo("private val foo = 5")
    assertThat(
            ktFile
                .findDescendantOfType<KtObjectDeclaration> { it.name == "Foo" }
                .isAncestor(results[0]))
        .isTrue
  }

  @Test
  fun `match on supertypes`() {
    val ktFile =
        load(
            """
          |object Foo : Super1 {}
          |object Bar : Super1, Super2 {}
          |object NoBar : Super2 {}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtClassOrObject>().apply {
              addCustomMatcher { it.superTypeListEntries.any { it.text == "Super1" } }
            })

    assertThat(results.map { it.name }).containsExactly("Foo", "Bar")
  }

  @Test
  fun `match on supertypes by typ reference`() {
    val ktFile =
        load(
            """
          |object Foo : Super1() {}
          |object Bar : Super1("a"), Super2("a") {}
          |object NoBar : Super2() {}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtClassOrObject>().apply {
              addCustomMatcher {
                it.superTypeListEntries.any { it.typeReference?.text == "Super1" }
              }
            })

    assertThat(results.map { it.name }).containsExactly("Foo", "Bar")
  }

  @Test
  fun `match on specific types of receiver and selector`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  (1 + 2).toFloat() // yes
          |  2.toFloat() // no
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtQualifiedExpression>().apply {
              addChildMatcher { it.receiverExpression is KtParenthesizedExpression }
              addChildMatcher { it.selectorExpression is KtCallExpression }
            })

    assertThat(results.map { it.text }).containsExactly("(1 + 2).toFloat()")
  }

  @Test
  fun `match on specific types of qualification`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  a?.b() // yes
          |  a.b() // no
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtQualifiedExpression>().apply {
              addChildMatcher { it.operationSign.value == "?." }
            })

    assertThat(results.map { it.text }).containsExactly("a?.b()")
  }

  @Test
  fun `match on value arguments`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  doIt("yay", num = 1) // yes
          |  doIt("boo", num = 1) // no
          |  doIt("yay", food = 1) // no
          |}
        """
                .trimMargin())
    val results =
        ktFile.findAll(
            match<KtCallExpression>().apply {
              addChildMatcher {
                it.valueArguments.getOrNull(0)?.getArgumentExpression()?.text == "\"yay\""
              }
              addChildMatcher {
                it.valueArguments.getOrNull(1)?.getArgumentName()?.asName?.identifier == "num"
              }
            })

    assertThat(results.map { it.text }).containsExactly("doIt(\"yay\", num = 1)")
  }

  @Test
  fun `replace an integer constant with +1`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  doIt(1)
          |  doIt(2)
          |  val a = 1
          |}
        """
                .trimMargin())

    val newKtFile =
        ktFile.replaceAllWithVariables(
            matcher =
                match<KtExpression>().apply { addChildMatcher { it.text?.toIntOrNull() != null } },
            replaceWith = { (it, _) -> (it.node.text.toInt() + 1).toString() })

    assertThat(newKtFile.text)
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

  @Test
  fun `match on binary expressions`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  println(1 + 2) // yes
          |  println(0 + 2) // no
          |  println(1 + 0) // no
          |  println(1 - 2) // no
          |}
        """
                .trimMargin())

    val results =
        ktFile.findAll(
            match<KtBinaryExpression>().apply {
              addChildMatcher { it.left?.text == "1" }
              addChildMatcher { it.right?.text == "2" }
              addChildMatcher { it.operationReference.text == "+" }
            })

    assertThat(results.map { it.text }).containsExactly("1 + 2")
  }

  @Test
  fun `match on unary expressions`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  i++ // yes
          |  i!! // no
          |  ++i // no
          |}
        """
                .trimMargin())

    val results =
        ktFile.findAll(
            match<KtUnaryExpression>().apply {
              addChildMatcher { it.baseExpression?.text == "i" }
              addChildMatcher { it.operationReference.text == "++" }
              addChildMatcher { it is KtPostfixExpression }
            })

    assertThat(results.map { it.text }).containsExactly("i++")
  }

  @Test
  fun `match on class literal expressions`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  println(Foo::class) // yes
          |  println(Foo::bar) // no
          |  println(Foo :: class) // yes
          |}
        """
                .trimMargin())

    val results =
        ktFile.findAll(
            match<KtClassLiteralExpression>().apply {
              addChildMatcher { it.receiverExpression?.text == "Foo" }
            })

    assertThat(results.map { it.text }).containsExactly("Foo::class", "Foo :: class")
  }

  @Test
  fun `replace when patches intersect`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  invoke(invoke(invoke() + invoke(2)))
          |}
        """
                .trimMargin())

    val newKtFile =
        ktFile.replaceAllWithVariables(
            matcher = match<KtCallExpression> { it.referenceExpression()?.text == "invoke" },
            replaceWith = { (it, _) -> it.node.text.replaceFirst("invoke", "apply") })

    assertThat(newKtFile.text)
        .isEqualTo(
            """
          |fun f() {
          |  apply(apply(apply() + apply(2)))
          |}
        """
                .trimMargin())
  }

  @Test(expected = IllegalArgumentException::class)
  fun `replace when patches intersect and throw`() {
    val ktFile =
        load(
            """
          |fun f() {
          |  println(invoke(invoke()))
          |}
        """
                .trimMargin())

    ktFile.replaceAllWithVariables(
        matcher = match<KtCallExpression> { it.referenceExpression()?.text == "invoke" },
        replaceWith = { (it, _) -> it.node.text.replaceFirst("invoke", "${it.text} + ${it.text}") })
  }
}
