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

import org.jetbrains.kotlin.com.intellij.psi.PsiExpressionList
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.junit.Test

/** Tests [AValueArgumentList] */
class AValueArgumentListTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AValueArgumentList, PsiExpressionList, KtValueArgumentList>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AValueArgumentList>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    return f(a, b);
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    return f(name = a, b)
                |  }
                |}
                """
                    .trimMargin(),
        )

    for (aElement in listOf(javaElement, kotlinElement)) {
      aElementsTestUtil.assertSamePsiElement(
          aElement,
          { it.rightParenthesis },
          { it.lastChild },
          { it.rightParenthesis },
      )
      aElementsTestUtil.assertSamePsiElement(
          aElement,
          { it.leftParenthesis },
          { it.firstChild },
          { it.leftParenthesis },
      )
      aElementsTestUtil.assertSamePsiElementList(
          aElement,
          { it.valueArguments },
          { it.expressions.toList() },
          { it.arguments.map { it.getArgumentExpression() } },
      )
    }
  }
}
