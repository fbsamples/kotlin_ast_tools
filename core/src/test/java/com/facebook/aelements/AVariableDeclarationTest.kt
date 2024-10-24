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

import org.jetbrains.kotlin.com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.junit.Test

/** Tests [AVariableDeclaration] */
class AVariableDeclarationTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AVariableDeclaration, PsiVariable, KtCallableDeclaration>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AVariableDeclaration>(
            javaCode =
                """
                |public class TestClass {
                |  int a = 5;
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  val a = 5
                |}
                """
                    .trimMargin())

    for (aElement in listOf(javaElement, kotlinElement)) {
      aElementsTestUtil.assertSamePsiElement(
          aElement, { it.typeReference }, { it.typeElement }, { it.typeReference })
    }
  }
}
