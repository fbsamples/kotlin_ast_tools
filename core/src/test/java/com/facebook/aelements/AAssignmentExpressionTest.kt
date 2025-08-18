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

import org.jetbrains.kotlin.com.intellij.psi.PsiAssignmentExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.junit.Test

/** Tests [AAssignmentExpression] */
class AAssignmentExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AAssignmentExpression, PsiAssignmentExpression, KtBinaryExpression>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AAssignmentExpression>(
            javaCode =
                """
                |public class TestClass {
                |
                |  int a = 0;
                |  
                |  public void doIt() {
                |    a = 5;
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |
                |  var a: Int = 0
                |
                |  fun doIt() {
                |    a = 5
                |  }
                |}
                """
                    .trimMargin(),
        )

    for (aElement in listOf(javaElement, kotlinElement)) {
      aElementsTestUtil.assertSamePsiElement(aElement, { it.left }, { it.lExpression }, { it.left })
      aElementsTestUtil.assertSamePsiElement(
          aElement,
          { it.right },
          { it.rExpression },
          { it.right },
      )
    }
  }
}
