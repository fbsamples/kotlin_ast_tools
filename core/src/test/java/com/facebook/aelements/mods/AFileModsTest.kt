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

package com.facebook.aelements.mods

import com.facebook.aelements.ACallExpression
import com.facebook.aelements.AFile
import com.facebook.aelements.collectDescendantsOfType
import com.facebook.aelements.util.AElementTestingUtil
import com.facebook.tools.codemods.writer.psi.PsiWriter
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class AFileModsTest {
  @Test
  fun `test replaceInPlace`() {
    val aElementsTestUtil = AElementTestingUtil<AFile, PsiJavaFile, KtFile>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AFile>(
            javaCode =
                """
                |public class TestClass {
                |  public void foo(int a) {
                |    print(a);
                |  }
                |  
                |  public void doIt(int a, int b) {
                |    foo(a + b);
                |  }
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |class TestClass {
                |  fun foo(a: Int) {
                |    print(a)
                |  }
                |
                |  fun doIt(a: Int, b: Int) {
                |    foo(a + b)
                |  }
                |}
                """
                    .trimMargin(),
        )
    listOf(javaElement, kotlinElement).forEach { aElement ->
      val psiWriter: PsiWriter = mock()
      aElement.replaceInPlace<ACallExpression>(
          matcher = { it.unqualifiedCalleeName == "foo" },
          replacement = { "bar${it.valueArgumentList?.text}" },
          imports = listOf("com.meta.TestClass"),
          psiWriter = psiWriter,
      )
      verify(psiWriter).addImport("com.meta.TestClass")
      aElement.collectDescendantsOfType<ACallExpression>().forEach { call ->
        if (call.unqualifiedCalleeName == "foo") {
          verify(psiWriter).changeTo(call, "bar(a + b)")
        } else {
          verify(psiWriter, never()).changeTo(eq(call), any())
        }
      }
    }
  }
}
