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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.kotlin.com.intellij.psi.PsiLocalVariable
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiThisExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtThisExpression
import org.junit.Test

/** Tests [PsiTestUtils] */
class PsiTestUtilsTest {

  @Test
  fun `test requireSingle in Kotlin`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |fun f() {
            |  doIt(1)
            |  doIt(2)
            |  val a = 1
            |}
            |
            |fun f(n: Int) {} 
            """
                .trimMargin()
        )

    assertThat(ktFile.requireSingle(text = "doIt(1)")).isInstanceOf(KtCallExpression::class.java)
    assertThat(ktFile.requireSingle(name = "a")).isInstanceOf(KtProperty::class.java)
    assertThat(ktFile.requireSingleOfType<KtProperty>().text).isEqualTo("val a = 1")

    assertThatThrownBy { ktFile.requireSingle(name = "f") }
        .hasMessage(
            """
            |Expected exactly one element to match name="f" under element, but found 2 elements:
            |1) fun f() {
            |  doIt(1)
            |  doIt(2)
            |  val a = 1
            |}
            |2) fun f(n: Int) {}
            """
                .trimMargin()
        )
    assertThatThrownBy { ktFile.requireSingle(name = "nonExistent") }
        .hasMessage(
            "Expected exactly one element to match name=\"nonExistent\" under element, but found no elements"
        )
    assertThatThrownBy { ktFile.requireSingleOfType<KtThisExpression>() }
        .hasMessage(
            "Expected exactly one element to match type=KtThisExpression under element, but found no elements"
        )
  }

  @Test
  fun `test requireSingle in Java`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |class Example {
            |  public static void f() {
            |    doIt(1);
            |    doIt(2);
            |    int a = 1;
            |  }
            |  
            |  public static void f(int n) {}
            |}
            """
                .trimMargin()
        )

    assertThat(psiJavaFile.requireSingle("doIt(1)"))
        .isInstanceOf(PsiMethodCallExpression::class.java)
    assertThat(psiJavaFile.requireSingle(name = "a")).isInstanceOf(PsiLocalVariable::class.java)
    assertThat(psiJavaFile.requireSingleOfType<PsiLocalVariable>().text).isEqualTo("int a = 1;")

    assertThatThrownBy { psiJavaFile.requireSingle(name = "f") }
        .hasMessage(
            """
            |Expected exactly one element to match name="f" under element, but found 2 elements:
            |1) public static void f() {
            |    doIt(1);
            |    doIt(2);
            |    int a = 1;
            |  }
            |2) public static void f(int n) {}
            """
                .trimMargin()
        )
    assertThatThrownBy { psiJavaFile.requireSingle(name = "nonExistent") }
        .hasMessage(
            "Expected exactly one element to match name=\"nonExistent\" under element, but found no elements"
        )
    assertThatThrownBy { psiJavaFile.requireSingleOfType<PsiThisExpression>() }
        .hasMessage(
            "Expected exactly one element to match type=PsiThisExpression under element, but found no elements"
        )
  }
}
