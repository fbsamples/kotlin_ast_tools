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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLocalVariable
import org.assertj.core.api.Assertions.assertThat
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
                .trimMargin())

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
  fun `test find usages in both languages`() {
    val aFiles =
        listOf(
            KotlinParserUtil.parseAsFile(
                    """
        |fun doIt(name: String) {
        |  println(name)
        |}
        """
                        .trimMargin())
                .toAElement(),
            JavaPsiParserUtil.parseAsFile(
                    """
        |public class Example {
        |  public static String doIt(String name) {
        |    println(name);
        |  }
        |}
        """
                        .trimMargin())
                .toAElement())
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
                .trimMargin())

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
                .trimMargin())

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
        |fun doIt(boolean b) {
        |  val n = 5
        |  if (n < 2 && b) {
        |    n = 6
        |  } else {
        |    n--
        |  }
        |}
        """
                .trimMargin())

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
                        .trimMargin())
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
                        .trimMargin())
                .toAElement())
    for (aFile in aFiles) {
      val usages =
          UsagesFinder.getWrites(
              aFile.findDescendantOfType<AVariableDeclaration> { it.name == "a" }!!)
      assertThat(usages.map { it.text }.getOrNull(1)).isEqualTo("a = name")
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
