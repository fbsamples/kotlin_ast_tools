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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiStatement
import java.lang.IllegalStateException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.Test

/** Tests [JavaPsiParserUtil] */
class JavaPsiParserUtilTest {

  @Test
  fun `parse as file returns a KtFile with correct class name`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
      |package com.facebook.foo
      |
      |class Foo {
      |  int a = 1;
      |}
    """
                .trimMargin())
    val classOrObject = javaPsiFile.findDescendantOfType<PsiClass>()
    assertThat(classOrObject).isNotNull
    assertThat(classOrObject?.name).isEqualTo("Foo")
  }

  @Test
  fun `parse as class`() {
    val clazz =
        JavaPsiParserUtil.parseAsClassOrInterface(
            """
      |class Foo {
      |  int a = 1;
      |}
    """
                .trimMargin())
    assertThat(clazz).isInstanceOf(PsiClass::class.java)
    assertThat(clazz.isInterface).isFalse
    assertThat(clazz.name).isEqualTo("Foo")
  }

  @Test
  fun `parse as interface`() {
    val clazz =
        JavaPsiParserUtil.parseAsClassOrInterface(
            """
      |interface IFoo {
      |  public void doThing();
      |}
    """
                .trimMargin())
    assertThat(clazz).isInstanceOf(PsiClass::class.java)
    assertThat(clazz.isInterface).isTrue
    assertThat(clazz.name).isEqualTo("IFoo")
  }

  @Test
  fun `parse as method`() {
    val method =
        JavaPsiParserUtil.parseAsMethod(
            """
      |public void doThing() {
      |  String a = "hello";
      |}
    """
                .trimMargin())
    assertThat(method).isInstanceOf(PsiMethod::class.java)
    assertThat(method.name).isEqualTo("doThing")
    assertThat(method.parameters).isEmpty()
  }

  @Test
  fun `parse as statement`() {
    val statement = JavaPsiParserUtil.parseAsStatement("String a = \"hello\";")
    assertThat(statement).isInstanceOf(PsiStatement::class.java)
    assertThat(statement.text).isEqualTo("String a = \"hello\";")
  }

  @Test
  fun `parse as expression`() {
    val statement = JavaPsiParserUtil.parseAsExpression("doThing(a)")
    assertThat(statement).isInstanceOf(PsiExpression::class.java)
    assertThat(statement.text).isEqualTo("doThing(a)")
  }

  @Test
  fun `when cannot parse give a good error message`() {
    try {
      JavaPsiParserUtil.parseAsStatement("@Hello")
      Assertions.fail("Expected exception")
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage("Cannot parse as PsiStatement: '@Hello'")
    }
  }
}
