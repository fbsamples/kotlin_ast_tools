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

import org.jetbrains.kotlin.com.intellij.psi.PsiIfStatement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.junit.Test

/** Tests [AIfExpressionOrStatement] */
class AIfExpressionOrStatementTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AIfExpressionOrStatement, PsiIfStatement, KtIfExpression>()

    val (javaAIfExpressionOrStatement, kotlinAIfExpressionOrStatement) =
        aElementsTestUtil.loadTestAElements<AIfExpressionOrStatement>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    if (a + b > 0) {
                |      return;
                |    } else {
                |      System.out.println("a + b <= 0");
                |    }
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    if (a + b > 0) {
                |      return
                |    } else {
                |      println("a + b <= 0")
                |    }
                |  }
                |}
                """
                    .trimMargin())

    for (aElement in listOf(javaAIfExpressionOrStatement, kotlinAIfExpressionOrStatement)) {
      aElementsTestUtil.assertSamePsiElement(
          aElement, { it.condition }, { it.condition }, { it.condition })
      aElementsTestUtil.assertSamePsiElement(aElement, { it.then }, { it.thenBranch }, { it.then })
      aElementsTestUtil.assertSamePsiElement(
          aElement, { it.`else` }, { it.elseBranch }, { it.`else` })
    }
  }
}
