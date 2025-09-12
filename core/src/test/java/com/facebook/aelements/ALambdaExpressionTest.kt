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
import org.jetbrains.kotlin.com.intellij.psi.PsiLambdaExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.junit.Test

/** Tests [ALambdaExpression] */
class ALambdaExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<ALambdaExpression, PsiLambdaExpression, KtLambdaExpression>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<ALambdaExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    log((a) -> a.toString());
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    log { a -> a.toString() }
                |  }
                |}
                """
                    .trimMargin(),
        )

    for (aElement in listOf(javaElement, kotlinElement)) {
      assertThat(aElement.psiElement.toAElement()).isInstanceOf(ALambdaExpression::class.java)
      assertThat(aElement.valueParameters.size).isEqualTo(1)
      assertThat(aElement.valueParameters.first().name).isEqualTo("a")
      aElementsTestUtil.assertSameString(
          aElement = aElement,
          onAElement = { it.bodyExpression?.text },
          onJava = { "a.toString()" },
          onKotlin = { "a.toString()" },
      )
    }
  }
}
