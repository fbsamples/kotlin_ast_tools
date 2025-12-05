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
import org.jetbrains.kotlin.com.intellij.psi.PsiThrowStatement
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.junit.Test

/** Tests [AThrowExpression] */
class AThrowExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AThrowExpression, PsiThrowStatement, KtThrowExpression>()

    val (javaAThrowExpression, kotlinAThrowExpression) =
        aElementsTestUtil.loadTestAElements<AThrowExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt() {
                |    throw new RuntimeException("error");
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt() {
                |    throw RuntimeException("error")
                |  }
                |}
                """
                    .trimMargin(),
        )

    for (aElement in listOf(javaAThrowExpression, kotlinAThrowExpression)) {
      aElementsTestUtil.assertSamePsiElement(
          aElement,
          { it.exception },
          { it.exception },
          { it.thrownExpression },
      )
    }
  }
}
