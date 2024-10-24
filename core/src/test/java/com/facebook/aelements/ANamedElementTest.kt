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

import org.jetbrains.kotlin.com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.junit.Test

/** Tests [ANamedElement] */
class ANamedElementTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<ANamedElement, PsiNamedElement, KtNamedDeclaration>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<ANamedElement>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    return a + b;
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    return a + b
                |  }
                |}
                """
                    .trimMargin())

    for (aElement in listOf(javaElement, kotlinElement)) {
      aElementsTestUtil.assertSameString(aElement, { it.name }, { it.name }, { it.name })
    }
  }
}
