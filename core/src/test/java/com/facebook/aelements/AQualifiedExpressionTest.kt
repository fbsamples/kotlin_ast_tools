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
import com.facebook.asttools.KotlinParserUtil
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.PsiExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.junit.Test

/** Tests [AQualifiedExpression] */
class AQualifiedExpressionTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AQualifiedExpression, PsiExpression, KtQualifiedExpression>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AQualifiedExpression>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    print(foo.bar.num);
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    print(foo.bar.num)
                |  }
                |}
                """
                    .trimMargin(),
        )

    for (aElement in listOf(javaElement, kotlinElement)) {
      assertThat(aElement.psiElement.toAElement()).isInstanceOf(AQualifiedExpression::class.java)
      aElementsTestUtil.assertSameString(
          aElement = aElement,
          onAElement = { it.operator },
          onJava = { "." },
          onKotlin = { it.operationTokenNode.text },
      )
      aElementsTestUtil.assertSamePsiElement(
          aElement = aElement,
          onAElement = { it.receiverExpression },
          onJava = { (it as PsiReferenceExpression).qualifierExpression },
          onKotlin = { it.receiverExpression },
      )
      aElementsTestUtil.assertSamePsiElement(
          aElement = aElement,
          onAElement = { it.selectorExpression },
          onJava = { (it as PsiReferenceExpression).element },
          onKotlin = { it.selectorExpression },
      )
    }
    assertThat(javaElement.javaQualifiedExpression?.text).isEqualTo("foo.bar.num")
    assertThat(kotlinElement.javaQualifiedExpression?.text).isNull()
  }

  @Test
  fun `in Kotlin if expression works as reciever`() {
    val kotlinAElement =
        KotlinParserUtil.parseAsFile(
                """
                |fun doIt(n: Int) {
                |  if (n < 5) 1 else { 2 + 2 }.toString()
                """
                    .trimIndent()
            )
            .toAElement()
            .findDescendantOfType<AQualifiedExpression>()!!

    assertThat(kotlinAElement.receiverExpression.text).isEqualTo("if (n < 5) 1 else { 2 + 2 }")
  }
}
