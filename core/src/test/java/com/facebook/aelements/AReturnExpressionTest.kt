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

import com.facebook.aelements.util.AElementTestingUtil
import org.jetbrains.kotlin.com.intellij.psi.PsiReturnStatement
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.junit.Test

/** Tests [AReturnExpression] */
class AReturnExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AReturnExpression, PsiReturnStatement, KtReturnExpression>()

    val (javaAReturnExpression, kotlinAReturnExpression) =
        aElementsTestUtil.loadTestAElements<AReturnExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public int doIt() {
                |    return 42;
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(): Int {
                |    return 42
                |  }
                |}
                """
                    .trimMargin(),
        )

    for (aElement in listOf(javaAReturnExpression, kotlinAReturnExpression)) {
      aElementsTestUtil.assertSamePsiElement(
          aElement,
          { it.returnValue },
          { it.returnValue },
          { it.returnedExpression },
      )
    }
  }
}
