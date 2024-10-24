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

import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.junit.Test

/** Tests [AClassOrObject] */
class AClassOrObjectTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil = AElementTestingUtil<AClassOrObject, PsiClass, KtClassOrObject>()

    val (javaAClassOrObject, kotlinAClassOrObject) =
        aElementsTestUtil.loadTestAElements<AClassOrObject>(
            javaCode =
                """
                |public class TestClass {
                |  int c = 5;
                |  public void doIt(int a, int b) {
                |    return a + b;
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  val c = 5
                |  fun doIt(a: Int, b: Int) {
                |    return a + b
                |  }
                |}
                """
                    .trimMargin())

    for (aElement in listOf(javaAClassOrObject, kotlinAClassOrObject)) {
      aElementsTestUtil.assertSameString(aElement, { it.name }, { it.name }, { it.name })
      aElementsTestUtil.assertSamePsiElementList(
          aElement, { it.annotations }, { it.annotations.toList() }, { it.annotations })
      aElementsTestUtil.assertSamePsiElementList(
          aElement,
          { it.methods },
          { it.methods.toList() },
          { it.declarations.filterIsInstance<KtFunction>() })
      aElementsTestUtil.assertSamePsiElementList(
          aElement,
          { it.properties },
          { it.fields.toList() },
          { it.declarations.filterIsInstance<KtProperty>() })
    }
  }
}
