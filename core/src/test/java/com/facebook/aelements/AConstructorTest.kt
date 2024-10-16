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

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtConstructor
import org.junit.Test

/** Tests [AConstructor] */
class AConstructorTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil = AElementTestingUtil<AConstructor, PsiMethod, KtConstructor<*>>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AConstructor>(
            javaCode =
                """
                |public class TestClass {
                |
                |  private int sum;
                |  
                |  public TestClass(int a, int b) {
                |    sum = a + b;
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass(a: Int, b: Int) {
                |  private val sum = a + b
                |}
                """
                    .trimMargin())

    for (aElement in listOf(javaElement, kotlinElement)) {
      aElementsTestUtil.assertSamePsiElementList(
          aElement,
          { it.valueParameters },
          { it.parameterList.parameters.toList() },
          { it.valueParameters })
    }
  }
}
