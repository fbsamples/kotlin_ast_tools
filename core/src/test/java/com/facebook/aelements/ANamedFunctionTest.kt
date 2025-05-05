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

package com.facebook.aelements

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.Test

/** Tests [ANamedFunction] */
class ANamedFunctionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil = AElementTestingUtil<ANamedFunction, PsiMethod, KtNamedFunction>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<ANamedFunction>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    return a + b;
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    return a + b
                |  }
                |}
                """
                    .trimMargin())

    for (aElement in listOf(javaElement, kotlinElement)) {
      aElementsTestUtil.assertSamePsiElement(
          aElement, { it.typeReference }, { it.returnTypeElement }, { it.typeReference })
      aElementsTestUtil.assertSameString(aElement, { it.name }, { "doIt" }, { "doIt" })
      aElementsTestUtil.assertSamePsiElementList(
          aElement,
          { it.valueParameters },
          { it.parameterList.parameters.toList() },
          { it.valueParameters })
    }
  }

  @Test
  fun `test isOverride`() {
    val aElementsTestUtil = AElementTestingUtil<ANamedFunction, PsiMethod, KtNamedFunction>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AClassOrObject>(
            javaCode =
                """
                |import com.facebook.inject.statics.OverrideStatic;
                |
                |public class TestClass {
                |  @Override public void foo() {}
                |
                |  @OverrideStatic public void bar() {}
                |
                |  public void normal() {}
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |import com.facebook.inject.statics.OverrideStatic
                |
                |class TestClass {
                |  override fun foo() {}
                |
                |  @OverrideStatic fun bar() {}
                |  
                |  fun normal() {}
                |}
                """
                    .trimMargin())

    for (aElement: AClassOrObject in listOf(javaElement, kotlinElement)) {
      val overrideFunction = aElement.methods.single { it.name == "foo" }
      val staticallyOverrideFunction = aElement.methods.single { it.name == "bar" }
      val nonOverrideFunction = aElement.methods.single { it.name == "normal" }
      assertThat(overrideFunction.isOverride).isTrue()
      assertThat(overrideFunction.isOverrideStatic).isFalse()
      assertThat(staticallyOverrideFunction.isOverride).isFalse()
      assertThat(staticallyOverrideFunction.isOverrideStatic).isTrue()
      assertThat(nonOverrideFunction.isOverride).isFalse()
      assertThat(nonOverrideFunction.isOverrideStatic).isFalse()
    }
  }

  @Test
  fun `test isStatic`() {
    val aElementsTestUtil = AElementTestingUtil<ANamedFunction, PsiMethod, KtNamedFunction>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AClassOrObject>(
            javaCode =
                """
                |public class TestClass {
                |  public static void staticFun(int a, int b) {}
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  @JvmStatic
                |  fun staticFun(a: Int, b: Int) {}
                |
                |  companion object {
                |    fun companionFun(a: Int, b: Int) {}
                |  }
                |}
                """
                    .trimMargin())

    for (aElement: AClassOrObject in listOf(javaElement, kotlinElement)) {
      val staticFun = aElement.methods.single { it.name == "staticFun" }
      val companionFun = aElement.methods.filter { it.name == "companionFun" }
      assertThat(staticFun.isStatic).isTrue()
      assertThat(companionFun.map { it.isStatic }).doesNotContain(false)
    }
  }
}
