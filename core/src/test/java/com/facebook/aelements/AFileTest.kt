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

import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Test

/** Tests [AFile] */
class AFileTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil = AElementTestingUtil<AFile, PsiJavaFile, KtFile>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AFile>(
            javaCode =
                """
                |package com.facebook.foo;
                |
                |public class TestClass {}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |package com.facebook.foo
                |
                |class TestClass
                """
                    .trimMargin())

    for (aElement in listOf(javaElement, kotlinElement)) {
      aElementsTestUtil.assertSameString(
          aElement, { it.packageName }, { "com.facebook.foo" }, { "com.facebook.foo" })
    }
  }
}
