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

import com.facebook.asttools.JavaPsiParserUtil
import com.facebook.asttools.KotlinParserUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Tests [ATypeParameterListOwner] */
class ATypeParameterListOwnerTest {
  @Test
  fun `basic functionality`() {
    val (javaElement, kotlinElement) =
        JavaPsiParserUtil.parseAsFile(
                """
                |public class TestClass<ClassTypeParam> {
                |  public <FunctionParam> FunctionParam foo() { return null; }
                |}
                """
                    .trimMargin()
            )
            .toAElement() to
            KotlinParserUtil.parseAsFile(
                    """
                |open class TestClass<ClassTypeParam> {
                |  fun <FunctionParam> foo(): FunctionParam { return null }
                |}
                """
                        .trimMargin()
                )
                .toAElement()

    for (aFile in listOf(javaElement, kotlinElement)) {
      val testClass =
          aFile.collectDescendantsOfType<AClassOrObject>().firstOrNull { it.name == "TestClass" }
      assertThat(testClass?.typeParameters).size().isEqualTo(1)
      assertThat(testClass?.typeParameters?.map { it.text }).containsExactly("ClassTypeParam")

      val fooFunction =
          testClass?.collectDescendantsOfType<ANamedFunction>()?.firstOrNull { it.name == "foo" }
      assertThat(fooFunction?.typeParameters).size().isEqualTo(1)
      assertThat(fooFunction?.typeParameters?.map { it.text }).containsExactly("FunctionParam")
    }
  }
}
