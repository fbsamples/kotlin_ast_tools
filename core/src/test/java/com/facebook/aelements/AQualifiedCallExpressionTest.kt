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

import com.intellij.psi.PsiMethodCallExpression
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.junit.Test

/** Tests [AQualifiedCallExpression] */
class AQualifiedCallExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<
            AQualifiedCallExpression, PsiMethodCallExpression, KtQualifiedExpression>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AQualifiedCallExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    foo.bar.invoke(a, b);
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    foo.bar.invoke(a, b)
                |  }
                |}
                """
                    .trimMargin())

    for (aElement in listOf(javaElement, kotlinElement)) {
      assertThat(aElement.psiElement.toAElement())
          .isInstanceOf(AQualifiedCallExpression::class.java)
      aElementsTestUtil.assertSameString(
          aElement = aElement,
          onAElement = { it.operator },
          onJava = { "." },
          onKotlin = { it.operationTokenNode.text })
      aElementsTestUtil.assertSamePsiElement(
          aElement = aElement,
          onAElement = { it.receiverExpression },
          onJava = { it.methodExpression.qualifierExpression },
          onKotlin = { it.receiverExpression })
      aElementsTestUtil.assertSamePsiElementList(
          aElement = aElement,
          onAElement = { it.valueArguments },
          onJava = { it.argumentList.expressions.toList() },
          onKotlin = {
            (it.selectorExpression as KtCallExpression).valueArguments.map {
              it.getArgumentExpression()
            }
          })
      aElementsTestUtil.assertSameString(
          aElement = aElement,
          onAElement = { it.unqualifiedCalleeName },
          onJava = { "invoke" },
          onKotlin = { "invoke" })
    }
    assertThat(javaElement.javaQualifiedExpression?.text).isEqualTo("foo.bar.invoke")
    assertThat(kotlinElement.javaQualifiedExpression?.text).isNull()
    assertThat(javaElement.callExpressionKotlinElement?.text).isNull()
    assertThat(kotlinElement.callExpressionKotlinElement?.text).isEqualTo("invoke(a, b)")
  }
}
