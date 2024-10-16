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

import com.intellij.psi.PsiReferenceParameterList
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.junit.Test

/** Tests [ATypeArgumentList] */
class ATypeArgumentListTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<ATypeArgumentList, PsiReferenceParameterList, KtTypeArgumentList>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<ATypeArgumentList>(
            javaCode =
                """
                |public class TestClass {
                |  public void doIt(int a, int b) {
                |    return Foo.<A, B>f(a, b);
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun doIt(a: Int, b: Int) {
                |    return f<A, B>(a, b)
                |  }
                |}
                """
                    .trimMargin())

    for (aElement in listOf(javaElement, kotlinElement)) {
      aElementsTestUtil.assertSamePsiElementList(
          aElement,
          { it.typeArguments },
          { it.typeParameterElements.toList() },
          { it.arguments.map { it.typeReference } })
    }
  }
}
