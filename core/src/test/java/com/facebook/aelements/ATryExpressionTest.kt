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
import org.jetbrains.kotlin.com.intellij.psi.PsiTryStatement
import org.jetbrains.kotlin.psi.KtTryExpression
import org.junit.Test

/** Tests [ATryExpression] */
class ATryExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil = AElementTestingUtil<ATryExpression, PsiTryStatement, KtTryExpression>()

    val (javaATryExpression, kotlinATryExpression) =
        aElementsTestUtil.loadTestAElements<ATryExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt() {
                |    try {
                |      System.out.println("trying");
                |    } catch (Exception e) {
                |      System.out.println("caught");
                |    } finally {
                |      System.out.println("finally");
                |    }
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt() {
                |    try {
                |      println("trying")
                |    } catch (e: Exception) {
                |      println("caught")
                |    } finally {
                |      println("finally")
                |    }
                |  }
                |}
                """
                    .trimMargin(),
        )

    for (aElement in listOf(javaATryExpression, kotlinATryExpression)) {
      aElementsTestUtil.assertSamePsiElement(
          aElement,
          { it.tryBlock },
          { it.tryBlock },
          { it.tryBlock },
      )
      assert(aElement.catchClauses.isNotEmpty()) { "Expected catch clauses to be non-empty" }
      aElementsTestUtil.assertSamePsiElement(
          aElement,
          { it.finallyBlock },
          { it.finallyBlock },
          { it.finallyBlock?.finalExpression },
      )
    }
  }
}
