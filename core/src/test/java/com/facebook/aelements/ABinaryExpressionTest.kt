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

import org.jetbrains.kotlin.com.intellij.psi.PsiBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.junit.Test

/** Tests [ABinaryExpression] */
class ABinaryExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<ABinaryExpression, PsiBinaryExpression, KtBinaryExpression>()

    val (javaABinaryExpression, kotlinABinaryExpression) =
        aElementsTestUtil.loadTestAElements<ABinaryExpression>(
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
                    .trimMargin(),
        )

    for (aElement in listOf(javaABinaryExpression, kotlinABinaryExpression)) {
      aElementsTestUtil.assertSamePsiElement(aElement, { it.left }, { it.lOperand }, { it.left })
      aElementsTestUtil.assertSamePsiElement(aElement, { it.right }, { it.rOperand }, { it.right })
      aElementsTestUtil.assertSameString(
          aElement,
          { it.operator },
          { it.operationSign.text },
          { it.operationReference.text },
      )
    }
  }
}
