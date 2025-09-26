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

import com.facebook.aelements.AParameter
import com.facebook.aelements.AVariableDeclaration
import com.facebook.aelements.findDescendantOfType
import com.facebook.aelements.toAElement
import com.facebook.asttools.JavaPsiParserUtil
import com.facebook.asttools.KotlinParserUtil
import com.facebook.asttools.requireSingleOfType
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiLocalVariable
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.junit.Test

/** Tests [UsagesFinder] */
class UsagesFinderTest {

  @Test
  fun `test find usages in Kotlin`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.facebook.example
            |
            |fun doIt(name: String): String {
            |  val name = name.trim()
            |  println(name)
            |  val name2 = name + "!"
            |  return if (name2 > name) name2 else name  
            |}
            """
                .trimMargin()
        )

    val ktProperty = ktFile.findDescendantOfType<KtProperty> { it.name == "name" }!!
    val usages = UsagesFinder.getUsages(ktProperty)
    assertThat(usages.map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly(
            "5:11:name",
            "6:15:name + \"!\"",
            "7:22:name2 > name",
            "7:39:name",
        )
  }

  @Test
  fun `test find usages of props of function type`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.facebook.example
            |
            |fun doIt(name: String): String {
            |  val f: (String) -> String = { s -> "hello" }
            |  return f("test")
            |}
            """
                .trimMargin()
        )

    val ktProperty = ktFile.findDescendantOfType<KtProperty> { it.name == "f" }!!
    val usages = UsagesFinder.getUsages(ktProperty)
    assertThat(usages.map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly("5:10:f(\"test\")")
  }

  @Test
  fun `test find usages with same name function in Kotlin`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.facebook.example
            |
            |fun f(name: String): String {
            |  val f = 5
            |  println(::f)
            |  println(f)
            |}
            """
                .trimMargin()
        )

    val ktProperty = ktFile.requireSingleOfType<KtProperty>("val f = 5")
    val ktNamedFunction = ktFile.requireSingleOfType<KtNamedFunction>(name = "f")
    assertThat(UsagesFinder.getUsages(ktProperty).map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly("6:11:f")
    assertThat(
            UsagesFinder.getUsages(ktNamedFunction).map { "${locationOf(it)}:${it.parent?.text}" }
        )
        .containsExactly("5:13:::f")
  }

  @Test
  fun `test finding usages with explicit this and super pointers in Kotlin`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.facebook.example
            |
            |class Example(private val name: String) {
            |  fun f(name: String) {
            |    println(name)
            |    println(this.name)
            |    println(this@Example.name)
            |    class C(val name: String) {
            |      fun g() {
            |        println(name)
            |        println(this.name)
            |        println(this@Example.name)
            |        println(this@C.name)
            |      }
            |    }
            |  }
            |}
            """
                .trimMargin()
        )
    val ktParameter = ktFile.requireSingleOfType<KtParameter>("private val name: String")
    assertThat(UsagesFinder.getUsages(ktParameter).map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly("6:13:this.name", "7:13:this@Example.name", "12:17:this@Example.name")
    val ktParameter2 = ktFile.requireSingleOfType<KtParameter>("val name: String")
    println(ktParameter2.text)
    println(ktParameter2.parent?.text)
    assertThat(UsagesFinder.getUsages(ktParameter2).map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly("10:17:name", "11:17:this.name", "13:17:this@C.name")
  }

  @Test
  fun `test find usages in both languages`() {
    val aFiles =
        listOf(
            KotlinParserUtil.parseAsFile(
                    """
                    |fun doIt(name: String) {
                    |  println(name)
                    |}
                    """
                        .trimMargin()
                )
                .toAElement(),
            JavaPsiParserUtil.parseAsFile(
                    """
                    |public class Example {
                    |  public static String doIt(String name) {
                    |    println(name);
                    |  }
                    |}
                    """
                        .trimMargin()
                )
                .toAElement(),
        )
    for (aFile in aFiles) {
      val usages =
          UsagesFinder.getUsages(aFile.findDescendantOfType<AParameter> { it.name == "name" }!!)
      assertThat(usages.map { it.text }).containsExactly("name")
    }
  }

  @Test
  fun `test find usages in Java`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.facebook.example;
            |
            |public class Example {
            |  public static String doIt(String name) {
            |    String name = name.trim();
            |    System.out.println(name);
            |    String name2 = name + "!";
            |    if (name.equals(name2)) {
            |      return name2;
            |    } else {
            |      return name;
            |    }  
            |  }
            |}
            """
                .trimMargin()
        )

    val psiVariable = psiJavaFile.findDescendantOfType<PsiLocalVariable> { it.name == "name" }!!
    val usages = UsagesFinder.getUsages(psiVariable)
    assertThat(usages.map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly(
            "6:24:(name)",
            "7:20:name + \"!\"",
            "8:9:name.equals",
            "11:14:return name;",
        )
  }

  @Test
  fun `test find usages with same name function in Java`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.facebook.example;
            |
            |import java.util.List;
            |public class Example {
            |  void f(String name) {
            |    int f = 5;
            |    new ArrayList<String>().forEach(this::f);
            |    println(f);
            |  }
            |}
            """
                .trimMargin()
        )

    val psiLocalVariable = psiJavaFile.requireSingleOfType<PsiLocalVariable>("int f = 5;")
    val psiMethod = psiJavaFile.requireSingleOfType<PsiMethod>(name = "f")
    assertThat(
            UsagesFinder.getUsages(psiLocalVariable).map { "${locationOf(it)}:${it.parent?.text}" }
        )
        .containsExactly("8:13:(f)")
    assertThat(UsagesFinder.getUsages(psiMethod).map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly("7:37:(this::f)")
  }

  @Test
  fun `test finding usages with explicit this and super pointers in Java`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.facebook.example;
            |
            |class Example {
            |
            |  private int a = 1;
            |
            |  public void f() {
            |    new Runnable() {
            |      private int a = 2;
            |
            |      public void run() {
            |        int a = 3;
            |        System.out.println(a);
            |        System.out.println(this.a);
            |        System.out.println(Example.this.a);
            |      }
            |    }.run();
            |  }
            |}
            """
                .trimMargin()
        )
    val psiVariable1 = psiJavaFile.requireSingleOfType<PsiVariable>("private int a = 1;")
    val psiVariable2 = psiJavaFile.requireSingleOfType<PsiVariable>("private int a = 2;")
    val psiVariable3 = psiJavaFile.requireSingleOfType<PsiVariable>("int a = 3;")
    assertThat(UsagesFinder.getUsages(psiVariable3).map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly("13:28:(a)")
    assertThat(UsagesFinder.getUsages(psiVariable2).map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly("14:28:(this.a)")
    assertThat(UsagesFinder.getUsages(psiVariable1).map { "${locationOf(it)}:${it.parent?.text}" })
        .containsExactly("15:28:(Example.this.a)")
  }

  @Test
  fun `test find writes in Java`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.facebook.example;
            |
            |public class Example {
            |  public static void doIt(boolean b) {
            |    int n = 5;
            |    if (n < 2 && b) {
            |      n = 6;
            |    } else {
            |      n--;
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val psiVariable = psiJavaFile.findDescendantOfType<PsiLocalVariable> { it.name == "n" }!!
    val usages = UsagesFinder.getWrites(psiVariable)
    assertThat(usages.map { "${locationOf(it)}:${it.text}" })
        .containsExactly(
            "5:5:int n = 5;",
            "7:7:n = 6",
            "9:7:n--",
        )
  }

  @Test
  fun `test find writes in Kotlin`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.facebook.example
            |
            |fun doIt(b: Boolean) {
            |  val n = 5
            |  if (n < 2 && b) {
            |    n = 6
            |  } else {
            |    n--
            |  }
            |}
            """
                .trimMargin()
        )

    val ktProperty = ktFile.findDescendantOfType<KtProperty> { it.name == "n" }!!
    val usages = UsagesFinder.getWrites(ktProperty)
    assertThat(usages.map { "${locationOf(it)}:${it.text}" })
        .containsExactly(
            "4:3:val n = 5",
            "6:5:n = 6",
            "8:5:n--",
        )
  }

  @Test
  fun `test find writes in both languages`() {
    val aFiles =
        listOf(
            KotlinParserUtil.parseAsFile(
                    """
                    |fun doIt(name: String) {
                    |  var a = "5"
                    |  a = name  
                    |}
                    """
                        .trimMargin()
                )
                .toAElement(),
            JavaPsiParserUtil.parseAsFile(
                    """
                    |public class Example {
                    |  public static String doIt(String name) {
                    |    String a = "5";
                    |    a = name;
                    |  }
                    |}
                    """
                        .trimMargin()
                )
                .toAElement(),
        )
    for (aFile in aFiles) {
      val usages =
          UsagesFinder.getWrites(
              aFile.findDescendantOfType<AVariableDeclaration> { it.name == "a" }!!
          )
      assertThat(usages.map { it.text }.getOrNull(1)).isEqualTo("a = name")
    }
  }

  @Test
  fun `test find reads in both languages`() {
    val aFiles =
        listOf(
            KotlinParserUtil.parseAsFile(
                    """
                    |fun doIt(name: String): String {
                    |  var a = "5"
                    |  a = name
                    |  return a  
                    |}
                    """
                        .trimMargin()
                )
                .toAElement(),
            JavaPsiParserUtil.parseAsFile(
                    """
                    |public class Example {
                    |  public static String doIt(String name) {
                    |    String a = "5";
                    |    a = name;
                    |    return a;
                    |  }
                    |}
                    """
                        .trimMargin()
                )
                .toAElement(),
        )
    for (aFile in aFiles) {
      val usages =
          UsagesFinder.getReads(
              aFile.findDescendantOfType<AVariableDeclaration> { it.name == "a" }!!
          )
      assertThat(usages.map { it.parent?.text }.single()).matches("return a;?")
    }
  }

  fun locationOf(psiElement: PsiElement): String {
    val file = psiElement.getTopmostParentOfType<PsiFile>()!!
    val substring = file.text.substring(0, psiElement.startOffset)
    val lineNumber = substring.count { it == '\n' } + 1
    val offsetInLine = psiElement.startOffset - substring.lastIndexOf('\n')
    return "$lineNumber:$offsetInLine"
  }
}
