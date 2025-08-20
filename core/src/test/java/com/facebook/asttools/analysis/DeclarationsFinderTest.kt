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

import com.facebook.asttools.JavaPsiParserUtil
import com.facebook.asttools.KotlinParserUtil
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiForeachStatement
import org.jetbrains.kotlin.com.intellij.psi.PsiLocalVariable
import org.jetbrains.kotlin.com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.com.intellij.psi.PsiReturnStatement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.Test

/** Tests [DeclarationsFinder] */
class DeclarationsFinderTest {

  @Test
  fun `finds a bunch of declarations in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt(name: String) {
        |    println("hello " + name)
        |  }
        |
        |  fun alsoDoThis(anotherName: String) {
        |    println("meh")
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }!!
            )
        )
        .containsOnlyKeys(
            "Foo",
            "alsoDoThis",
            "doIt",
            "name",
        )
    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println("meh")""" }!!
            )
        )
        .containsOnlyKeys(
            "Foo",
            "alsoDoThis",
            "anotherName",
            "doIt",
        )
  }

  @Test
  fun `find declarations from previous statements in blocks in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt(name: String) {
        |    val a = 5
        |    call {
        |      val a2 =7
        |    }
        |    println("hello " + name)
        |    val b = 6
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }!!
            )
        )
        .containsOnlyKeys(
            "Foo",
            "a",
            "doIt",
            "name",
        )
  }

  @Test
  fun `find declarations for variables with overloaded names in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(val name: String?) {
        |  fun doIt(name: String) {
        |    println("hello " + name)
        |  }
        |}
        """
                .trimMargin()
        )

    val declarations =
        DeclarationsFinder.getDeclarationsAt(
            ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }!!
        )
    assertThat(declarations)
        .containsOnlyKeys(
            "Foo",
            "doIt",
            "name",
        )

    val nameDeclarations = declarations["name"] as DeclarationsFinder.Overloaded
    assertThat(nameDeclarations.values.size).isEqualTo(2)
    assertThat(nameDeclarations.value.text).isEqualTo("name: String")
    assertThat(nameDeclarations._last.text).isEqualTo("val name: String?")
  }

  @Test
  fun `find declarations from loop parameters in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt(name: String) {
        |    for (a in listOf(1, 2)) {
        |      println(a + "hello " + name)
        |    }
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println(a + "hello " + name)""" }!!
            )
        )
        .containsOnlyKeys(
            "Foo",
            "a",
            "doIt",
            "name",
        )
  }

  @Test
  fun `declarations at for loop do not include loop parameter yet in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt(name: String) {
        |    for (a in listOf(1, 2)) {
        |      println(a + "hello " + name)
        |    }
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                checkNotNull(ktFile.findDescendantOfType<KtForExpression>())
            )
        )
        .containsOnlyKeys(
            "Foo",
            "doIt",
            "name",
        )
    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                checkNotNull(ktFile.findDescendantOfType<KtForExpression>()?.loopParameter)
            )
        )
        .containsOnlyKeys(
            "Foo",
            "doIt",
            "name",
        )
    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                checkNotNull(ktFile.findDescendantOfType<KtForExpression>()?.loopRange)
            )
        )
        .containsOnlyKeys(
            "Foo",
            "doIt",
            "name",
        )
  }

  @Test
  fun `find declarations in lambdas overriding other declarations in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt(name: String) {
        |    a = { name: Int -> println(name) }
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                    ktFile.findDescendantOfType { it.text == "println(name)" }!!
                )["name"]!!
                .value
                .text
        )
        .isEqualTo("name: Int")
  }

  @Test
  fun `find declaration for implicit it argument in lambda in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt(name: String?) {
        |    name?.let { println(it) }
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                    ktFile.findDescendantOfType { it.text == "println(it)" }!!
                )["it"]!!
                .value
                .text
        )
        .isEqualTo("{ println(it) }")
  }

  @Test
  fun `find declarations unnamed functions in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  val a = fun (name: String) {
        |    println(name)
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println(name)""" }!!
            )
        )
        .containsKeys("name")
  }

  @Test
  fun `find property declarations in primary constructor from inner method in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(val name: String) {
        |  fun doIt() {
        |    println("hello " + name)
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }!!
            )
        )
        .containsKeys("name")
  }

  @Test
  fun `do not find parameters in primary constructor from inner method in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(name: String) {
        |  fun doIt() {
        |    val name: Int = 5
        |    println("hello " + name)
        |  }
        |}
        """
                .trimMargin()
        )

    val map =
        DeclarationsFinder.getDeclarationsAt(
            ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }!!
        )
    assertThat(map).containsKey("name")
    assertThat(map["name"]?.value?.text).isEqualTo("val name: Int = 5")
  }

  @Test
  fun `find parameters in primary constructor in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(name: String) {
        |  val name2 = name
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """val name2 = name""" }!!
            )
        )
        .containsKey("name")
  }

  @Test
  fun `find parameters in primary constructor in init block in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(name: String) {
        |  init {
        |    println("hello " + name)
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }!!
            )
        )
        .containsKey("name")
  }

  @Test
  fun `find property declarations from a companion object in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt() {
        |    println("hello " + name)
        |  }
        |
        |  companion object {
        |    val name = "A"
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }!!
            )
        )
        .containsKeys("name")
  }

  @Test
  fun `find constructor parameter declarations from a property when parameter name is overloaded in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(context: Context) {
        |  val context: Context = applicationContext
        |  val bar: Bar = Bar(context)
        |}
        """
                .trimMargin()
        )

    val contextDeclaration =
        DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """val bar: Bar = Bar(context)""" }!!
            )["context"]
    assertThat(contextDeclaration?.value).isInstanceOf(KtParameter::class.java)
    assertThat(contextDeclaration?.value?.text).isEqualTo("context: Context")
  }

  @Test
  fun `find inner class declaration in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt() {
        |    println("hello " + Inner.name)
        |  }
        |
        |  object Inner {
        |    val name = "A"
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """println("hello " + Inner.name)""" }!!
            )
        )
        .containsKeys("Inner")
  }

  @Test
  fun `find smart cast limitations in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt(any: Any) {
        |    println("any")
        |    if (any is String) {
        |      println("string" + any)
        |    }
        |    if (any !is Int) {
        |      return
        |    }
        |    println("int" + any)
        |  }
        |}
        """
                .trimMargin()
        )

    val declaration = ktFile.findDescendantOfType<KtNamedDeclaration> { it.text == "any: Any" }!!
    val usage1 =
        ktFile.findDescendantOfType<KtSimpleNameExpression> {
          it.text == "any" && it.parent.text == """"string" + any"""
        }!!
    assertThat(DeclarationsFinder.findSmartCastConstraints(usage1, declaration).map { it.text })
        .containsExactly("any is String")

    val usage2 = ktFile.collectDescendantsOfType<KtSimpleNameExpression> { it.text == "any" }.last()
    assertThat(DeclarationsFinder.findSmartCastConstraints(usage2, declaration).map { it.text })
        .containsExactly("any !is Int")
  }

  @Test
  fun `find the closest declaration for a reference to a variable with an overloaded name in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(val name: String?) : SuperFoo(name!!) {
        |  fun doIt(name: String) {
        |    println("hello " + name)
        |  }
        |}
        """
                .trimMargin()
        )

    val functionParameterDeclaration =
        DeclarationsFinder.getDeclarationAt(
            checkNotNull(ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }),
            "name",
        )
    assertThat(functionParameterDeclaration).isInstanceOf(KtParameter::class.java)
    assertThat(functionParameterDeclaration?.text).isEqualTo("name: String")

    val constructorParameterDeclaration =
        DeclarationsFinder.getDeclarationAt(
            checkNotNull(ktFile.findDescendantOfType { it.text == """SuperFoo(name!!)""" }),
            "name",
        )
    assertThat(constructorParameterDeclaration).isInstanceOf(KtParameter::class.java)
    assertThat(constructorParameterDeclaration?.text).isEqualTo("val name: String?")
  }

  @Test
  fun `find the closest variable declaration for a reference with same function name in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |  fun doIt() : Interface {
        |    val name = "a"
        |    return object : Interface() {
        |      fun name() {
        |        return name
        |      }
        |    }
        |  }
        |}
        """
                .trimMargin()
        )

    val localVariableDeclaration =
        DeclarationsFinder.getVariableDeclarationAt(
            checkNotNull(
                ktFile
                    .findDescendantOfType<KtReturnExpression> { it.text == """return name""" }
                    ?.returnedExpression
            ),
            "name",
        )
    assertThat(localVariableDeclaration).isInstanceOf(KtProperty::class.java)
    assertThat(localVariableDeclaration?.text).isEqualTo("val name = \"a\"")
  }

  @Test
  fun `find the closest declaration in a constructor parameter for a reference in Kotlin file`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(private val name: String) {
        |  fun doIt() {
        |    println(name)
        |  }
        |}
        """
                .trimMargin()
        )

    val localVariableDeclaration =
        DeclarationsFinder.getVariableDeclarationAt(
            checkNotNull(
                ktFile.findDescendantOfType<KtCallExpression> { it.text == """println(name)""" }
            ),
            "name",
        )
    assertThat(localVariableDeclaration).isInstanceOf(KtParameter::class.java)
    assertThat(localVariableDeclaration?.text).isEqualTo("private val name: String")
  }

  @Test
  fun `find the closest declaration constructor argument hidden by property in Kotlin File`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo(name: String?) {
        |
        |  private val name = "this one"
        |  private val constructorNameReference = name + 1
        |
        |  init {
        |    println("ctor param" + name)
        |  }
        |
        |  val something: String
        |      get() = "property" + name
        |
        |  fun doIt() {
        |    println("hello " + name)
        |  }
        |}
        """
                .trimMargin()
        )

    val propertyDeclaration =
        DeclarationsFinder.getDeclarationAt(
            checkNotNull(ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }),
            "name",
        )
    assertThat(propertyDeclaration?.text).isEqualTo("private val name = \"this one\"")
    assertThat(propertyDeclaration).isInstanceOf(KtProperty::class.java)

    val constructorParamDeclaration =
        DeclarationsFinder.getDeclarationAt(
            checkNotNull(
                ktFile.findDescendantOfType { it.text == """println("ctor param" + name)""" }
            ),
            "name",
        )
    assertThat(constructorParamDeclaration?.text).isEqualTo("name: String?")
    assertThat(constructorParamDeclaration).isInstanceOf(KtParameter::class.java)

    assertThat(
            DeclarationsFinder.getDeclarationAt(
                checkNotNull(ktFile.findDescendantOfType { it.text == """"property" + name""" }),
                "name",
            )
        )
        .isSameAs(propertyDeclaration)

    assertThat(
            DeclarationsFinder.getDeclarationAt(
                checkNotNull(ktFile.findDescendantOfType { it.text == """name + 1""" }),
                "name",
            )
        )
        .isSameAs(constructorParamDeclaration)
  }

  @Test
  fun `find the closest declaration do not be confused by inner interfaces`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.facebook.example
        |
        |class Foo {
        |
        |  interface Confusing {
        |    private val name = "not this one"
        |  }
        |
        |  private val name = "this one"
        |  
        |  interface Confusing2 {
        |    private val name = "not this one"
        |  }
        |  
        |  companion object {
        |    private val number = 1
        |  }
        |
        |  fun doIt() {
        |    println("hello " + name)
        |    println("hello " + number)
        |  }
        |}
        """
                .trimMargin()
        )

    val propertyDeclaration =
        DeclarationsFinder.getDeclarationAt(
            checkNotNull(ktFile.findDescendantOfType { it.text == """println("hello " + name)""" }),
            "name",
        )
    assertThat(propertyDeclaration?.text).isEqualTo("private val name = \"this one\"")
    assertThat(propertyDeclaration).isInstanceOf(KtProperty::class.java)

    val companionObjectDeclaration =
        DeclarationsFinder.getDeclarationAt(
            checkNotNull(
                ktFile.findDescendantOfType { it.text == """println("hello " + number)""" }
            ),
            "number",
        )
    assertThat(companionObjectDeclaration?.text).isEqualTo("private val number = 1")
    assertThat(companionObjectDeclaration).isInstanceOf(KtProperty::class.java)
  }

  @Test
  fun `find parameter to setter in parent`() {

    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |package com.facebook.tools.editus.commands.custom.postprocesskotlinconversion
          |
          |class Foo() {
          |
          |   private var mFastPlayActive = false
          |
          |   var isFastPlayActive: Boolean
          |     get() = mFastPlayActive
          |     set(fastPlayActive) {
          |       if (mFastPlayActive != fastPlayActive) {
          |        mFastPlayActive = fastPlayActive
          |        notifyPropertyChanged()
          |       }
          |     }
          |  
          |  private fun notifyPropertyChanged() {
          |  
          |  }
          |}
          |"""
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                ktFile.findDescendantOfType { it.text == """mFastPlayActive != fastPlayActive""" }!!
            )
        )
        .containsKeys("fastPlayActive")
  }

  @Test
  fun `finds a bunch of declarations in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public void doIt(String name) {
        |    System.out.println("hello " + name);
        |  }
        |
        |  public void alsoDoThis(String anotherName) {
        |    System.out.println("meh");
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                psiJavaFile.findDescendantOfType {
                  it.text == """System.out.println("hello " + name)"""
                }!!
            )
        )
        .containsOnlyKeys(
            "Foo",
            "alsoDoThis",
            "doIt",
            "name",
        )
    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                psiJavaFile.findDescendantOfType { it.text == """System.out.println("meh")""" }!!
            )
        )
        .containsOnlyKeys(
            "Foo",
            "alsoDoThis",
            "anotherName",
            "doIt",
        )
  }

  @Test
  fun `find declarations from previous statements in blocks in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public void doIt(String name) {
        |    int a = 5;
        |    {
        |      int a2 =7;
        |    }
        |    System.out.println("hello " + name);
        |    int b = 6;
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                psiJavaFile.findDescendantOfType {
                  it.text == """System.out.println("hello " + name)"""
                }!!
            )
        )
        .containsOnlyKeys(
            "Foo",
            "a",
            "doIt",
            "name",
        )
  }

  @Test
  fun `find declarations for variables with overloaded names in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |
        |  private String name;
        |  
        |  public void doIt(String name) {
        |    System.out.println("hello " + name);
        |  }
        |}
        """
                .trimMargin()
        )

    val declarations =
        DeclarationsFinder.getDeclarationsAt(
            psiJavaFile.findDescendantOfType {
              it.text == """System.out.println("hello " + name)"""
            }!!
        )
    assertThat(declarations)
        .containsOnlyKeys(
            "Foo",
            "doIt",
            "name",
        )

    val nameDeclarations = declarations["name"] as DeclarationsFinder.Overloaded
    assertThat(nameDeclarations.values.size).isEqualTo(2)
    assertThat(nameDeclarations.value.text).isEqualTo("String name")
    assertThat(nameDeclarations._last.text).isEqualTo("private String name;")
  }

  @Test
  fun `find declarations from loop parameters in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |import java.util.Arrays;
        |
        |public class Foo {
        |  public void doIt(String name) {
        |    for (int b = 0; b < 3; b++) {
        |      for (int a : Arrays.asList(1, 2)) {
        |        System.out.println(a + "hello " + name);
        |      }
        |    }
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                psiJavaFile.findDescendantOfType {
                  it.text == """System.out.println(a + "hello " + name)"""
                }!!
            )
        )
        .containsOnlyKeys(
            "Foo",
            "a",
            "b",
            "doIt",
            "name",
        )
  }

  @Test
  fun `declarations at for loop do not include loop parameter yet in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public void doIt(String name) {
        |    for (int a : Arrays.asList(1, 2)) {
        |      System.out.println(a + "hello " + name);
        |    }
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                checkNotNull(psiJavaFile.findDescendantOfType<PsiForeachStatement>())
            )
        )
        .containsOnlyKeys(
            "Foo",
            "doIt",
            "name",
        )
    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                checkNotNull(psiJavaFile.findDescendantOfType<PsiForeachStatement>()?.iteratedValue)
            )
        )
        .containsOnlyKeys(
            "Foo",
            "doIt",
            "name",
        )
    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                checkNotNull(
                    psiJavaFile.findDescendantOfType<PsiForeachStatement>()?.iterationParameter
                )
            )
        )
        .containsOnlyKeys(
            "Foo",
            "doIt",
            "name",
        )
  }

  @Test
  fun `find declarations in lambdas overriding other declarations in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public void doIt(String name) {
        |    Function<Int, Void> a = (name) -> System.out.println(name);
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                    psiJavaFile.findDescendantOfType { it.text == "System.out.println(name)" }!!
                )["name"]!!
                .value
                .text
        )
        .isEqualTo("name")
  }

  @Test
  fun `find property declarations in primary constructor from inner method in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |
        |  private String name;
        |  
        |  public Foo(String name) {
        |    this.name = name;
        |  }
        |  
        |  public void doIt() {
        |    System.out.println("hello " + name);
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                psiJavaFile.findDescendantOfType {
                  it.text == """System.out.println("hello " + name)"""
                }!!
            )
        )
        .containsKeys("name")
  }

  @Test
  fun `do not find parameters in primary constructor from inner method in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public Foo(String name) {}
        |
        |  public void doIt() {
        |    int name = 5;
        |    System.out.println("hello " + name);
        |  }
        |}
        """
                .trimMargin()
        )

    val map =
        DeclarationsFinder.getDeclarationsAt(
            psiJavaFile.findDescendantOfType {
              it.text == """System.out.println("hello " + name)"""
            }!!
        )
    assertThat(map).containsKey("name")
    assertThat(map["name"]?.value?.text).isEqualTo("int name = 5;")
  }

  @Test
  fun `find parameters in primary constructor in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |
        |  final String name2;
        |  
        |  public Foo(String name) {
        |    name2 = name;
        |  } 
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                psiJavaFile.findDescendantOfType { it.text == """name2 = name;""" }!!
            )
        )
        .containsKey("name")
  }

  @Test
  fun `find constructor parameter declarations from a property when parameter name is overloaded in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public Foo(Context context) {}
        |
        |  Context context = applicationContext;
        |  Bar bar = new Bar(context);
        |}
        """
                .trimMargin()
        )

    val contextDeclaration =
        DeclarationsFinder.getDeclarationsAt(
                psiJavaFile.findDescendantOfType { it.text == """Bar bar = new Bar(context);""" }!!
            )["context"]
    assertThat(contextDeclaration?.value).isInstanceOf(PsiField::class.java)
    assertThat(contextDeclaration?.value?.text).isEqualTo("Context context = applicationContext;")
  }

  @Test
  fun `find inner class declaration in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public void doIt() {
        |    System.out.println("hello " + Inner.name);
        |  }
        |
        |  public class Inner {
        |    String name = "A";
        |  }
        |}
        """
                .trimMargin()
        )

    assertThat(
            DeclarationsFinder.getDeclarationsAt(
                psiJavaFile.findDescendantOfType {
                  it.text == """System.out.println("hello " + Inner.name)"""
                }!!
            )
        )
        .containsKeys("Inner")
  }

  @Test
  fun `find the closest declaration for a reference to a variable with an overloaded name in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo extends SuperFoo {
        |  
        |  public Foo(@A String name) {
        |    super(name);
        |  }
        |  
        |  public void doIt(String name) {
        |    System.out.println("hello " + name);
        |  }
        |}
        """
                .trimMargin()
        )

    val functionParameterDeclaration =
        DeclarationsFinder.getDeclarationAt(
            checkNotNull(
                psiJavaFile.findDescendantOfType {
                  it.text == """System.out.println("hello " + name)"""
                }
            ),
            "name",
        )
    assertThat(functionParameterDeclaration).isInstanceOf(PsiParameter::class.java)
    assertThat(functionParameterDeclaration?.text).isEqualTo("String name")

    val constructorParameterDeclaration =
        DeclarationsFinder.getDeclarationAt(
            checkNotNull(psiJavaFile.findDescendantOfType { it.text == """super(name)""" }),
            "name",
        )
    assertThat(constructorParameterDeclaration).isInstanceOf(PsiParameter::class.java)
    assertThat(constructorParameterDeclaration?.text).isEqualTo("@A String name")
  }

  @Test
  fun `find the closest variable declaration for a reference with same function name in Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public Interface doIt() {
        |    String name = "a";
        |    return new Interface() {
        |      void name() {
        |        return name;
        |      }
        |    };
        |  }
        |}
        """
                .trimMargin()
        )

    val localVariableDeclaration =
        DeclarationsFinder.getVariableDeclarationAt(
            checkNotNull(
                psiJavaFile
                    .findDescendantOfType<PsiReturnStatement> { it.text == """return name;""" }
                    ?.returnValue
            ),
            "name",
        )
    assertThat(localVariableDeclaration).isInstanceOf(PsiLocalVariable::class.java)
    assertThat(localVariableDeclaration?.text).isEqualTo("String name = \"a\";")
  }

  @Test
  fun `does not get confused by fields in another class in a Java file`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.facebook.example;
        |
        |public class Foo {
        |  public Interface doIt() {
        |    println("hello, name is not available here");
        |  }
        |
        |  public static class Inner {
        |    String name = "b";
        |  }
        |}
        """
                .trimMargin()
        )

    val localVariableDeclaration =
        DeclarationsFinder.getVariableDeclarationAt(
            checkNotNull(
                psiJavaFile.findDescendantOfType<PsiExpression> {
                  it.text == """"hello, name is not available here""""
                }
            ),
            "name",
        )
    assertThat(localVariableDeclaration).isNull()
  }
}
