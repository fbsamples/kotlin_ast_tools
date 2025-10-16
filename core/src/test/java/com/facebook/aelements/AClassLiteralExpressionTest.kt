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
import org.jetbrains.kotlin.com.intellij.psi.PsiClassObjectAccessExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.junit.Test

/** Tests [AClassLiteralExpression] */
class AClassLiteralExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<
            AClassLiteralExpression,
            PsiClassObjectAccessExpression,
            KtClassLiteralExpression,
        >()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AClassLiteralExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt() {
                |    Class<?> clazz = String.class;
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt() {
                |    val clazz = String::class
                |  }
                |}
                """
                    .trimMargin(),
        )

    for (aElement in listOf(javaElement, kotlinElement)) {
      assertThat(aElement.psiElement.toAElement()).isInstanceOf(AClassLiteralExpression::class.java)
      aElementsTestUtil.assertSamePsiElement(
          aElement = aElement,
          onAElement = { it.receiverExpression },
          onJava = { it.operand },
          onKotlin = { it.receiverExpression },
      )
    }
    assertThat(javaElement.receiverExpression.text).isEqualTo("String")
    assertThat(kotlinElement.receiverExpression.text).isEqualTo("String")
  }
}
