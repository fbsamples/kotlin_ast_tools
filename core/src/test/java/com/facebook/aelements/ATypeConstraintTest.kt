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

import com.facebook.asttools.KotlinParserUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Tests [ATypeConstraint] */
class ATypeConstraintTest {
  @Test
  fun `basic functionality`() {
    val aFile =
        KotlinParserUtil.parseAsFile(
                """
                |open class TestClass<ClassTypeParam> where ClassTypeParam : String {
                |  fun <FunctionParam> foo(): FunctionParam where FunctionParam : String { return null }
                |}
                """
                    .trimMargin())
            .toAElement()

    val testClass =
        aFile.collectDescendantsOfType<AClassOrObject>().firstOrNull { it.name == "TestClass" }
    assertThat(testClass?.typeConstraints).size().isEqualTo(1)
    assertThat(testClass?.typeConstraints?.map { it.text })
        .containsExactly("ClassTypeParam : String")

    val fooFunction =
        testClass?.collectDescendantsOfType<ANamedFunction>()?.firstOrNull { it.name == "foo" }
    assertThat(fooFunction?.typeConstraints).size().isEqualTo(1)
    assertThat(fooFunction?.typeConstraints?.map { it.text })
        .containsExactly("FunctionParam : String")
  }
}
