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
import org.jetbrains.kotlin.com.intellij.psi.PsiContinueStatement
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.junit.Test

/** Tests [AContinueExpression] */
class AContinueExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AContinueExpression, PsiContinueStatement, KtContinueExpression>()

    val (javaAContinueExpression, kotlinAContinueExpression) =
        aElementsTestUtil.loadTestAElements<AContinueExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt() {
                |    for (int i = 0; i < 10; i++) {
                |      continue;
                |    }
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt() {
                |    for (i in 0..9) {
                |      continue
                |    }
                |  }
                |}
                """
                    .trimMargin(),
        )

    // Both continue statements exist
    assert(javaAContinueExpression != null)
    assert(kotlinAContinueExpression != null)
  }
}
