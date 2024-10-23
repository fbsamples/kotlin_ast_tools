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
import org.jetbrains.kotlin.com.intellij.psi.PsiArrayAccessExpression
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.junit.Test

/** Tests [AArrayAccessExpression] */
class AArrayAccessExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<
            AArrayAccessExpression, PsiArrayAccessExpression, KtArrayAccessExpression>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AArrayAccessExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    m[a];
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    m[a, b]
                |  }
                |}
                """
                    .trimMargin())
    for (aElement in listOf(javaElement, kotlinElement)) {
      assertThat(aElement.psiElement.toAElement()).isInstanceOf(AArrayAccessExpression::class.java)
      aElementsTestUtil.assertSamePsiElement(
          aElement = aElement,
          onAElement = { it.arrayExpression },
          onJava = { it.arrayExpression },
          onKotlin = { it.arrayExpression })
      aElementsTestUtil.assertSamePsiElementList(
          aElement = aElement,
          onAElement = { it.indexExpressions },
          onJava = { listOf(it.indexExpression) },
          onKotlin = { it.indexExpressions })
      aElementsTestUtil.assertSameString(
          aElement = aElement,
          onAElement = { it.leftBracket.text },
          onJava = { "[" },
          onKotlin = { "[" })
      aElementsTestUtil.assertSameString(
          aElement = aElement,
          onAElement = { it.rightBracket.text },
          onJava = { "]" },
          onKotlin = { "]" })
    }
  }
}
