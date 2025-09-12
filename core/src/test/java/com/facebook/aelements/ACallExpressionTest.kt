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
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.junit.Test

/** Tests [ACallExpression] */
class ACallExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<ACallExpression, PsiMethodCallExpression, KtExpression>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<ACallExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    invoke(a, b);
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    invoke(a, b)
                |  }
                |}
                """
                    .trimMargin(),
        )
    for (aElement in listOf(javaElement, kotlinElement)) {
      assertThat(aElement.psiElement.toAElement()).isInstanceOf(ACallExpression::class.java)
      aElementsTestUtil.assertSamePsiElementList(
          aElement = aElement,
          onAElement = { it.valueArguments },
          onJava = { it.argumentList.expressions.toList() },
          onKotlin = { (it as KtCallExpression).valueArguments.map { it.getArgumentExpression() } },
      )
    }
    assertThat(javaElement.callExpressionKotlinElement?.text).isNull()
    assertThat(kotlinElement.callExpressionKotlinElement?.text).isEqualTo("invoke(a, b)")
  }
}
