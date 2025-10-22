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
import org.jetbrains.kotlin.com.intellij.psi.PsiAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.junit.Test

class AAnnotationTest {

  @Test
  fun `annotation with arguments`() {
    val aElementsTestUtil = AElementTestingUtil<AAnnotation, PsiAnnotation, KtAnnotationEntry>()

    val (javaElement, kotlinElement) =
        aElementsTestUtil.loadTestAElements<AAnnotation>(
            javaCode =
                """
                |@Magic(value = 5, name = "test")
                |public class TestClass {
                |}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |@Magic(value = 5, name = "test")
                |class TestClass {
                |}
                """
                    .trimMargin(),
        )

    assertThat(javaElement.shortName).isEqualTo("Magic")
    assertThat(javaElement.valueArguments).hasSize(2)

    assertThat(kotlinElement.shortName).isEqualTo("Magic")
    assertThat(kotlinElement.valueArguments).hasSize(2)
  }
}
