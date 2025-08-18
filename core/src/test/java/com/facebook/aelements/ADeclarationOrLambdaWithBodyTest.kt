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

/** Tests [ADeclarationOrLambdaWithBody] */
class ADeclarationOrLambdaWithBodyTest {

  @Test
  fun `basic functionality`() {
    val psiJavaFile =
        JavaPsiParserUtil.parseAsFile(
            """
                |public class TestClass {
                |  public int a = 5;
                |  
                |  public TestClass() {}
                |
                |  public void doIt() {}
                |  
                |  Function1<Int, String> f = (a) -> a.toString();
                |}
                """
                .trimMargin())
    assertThat(
            psiJavaFile.toAElement().collectDescendantsOfType<ADeclarationOrLambdaWithBody>().map {
              it.text
            })
        .containsExactly("public TestClass() {}", "public void doIt() {}", "(a) -> a.toString()")

    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
                |class TestClass() {
                | 
                |  val a = 5
                |  
                |  fun doIt() {}
                |
                |  val b: Int
                |    get() { return 7 }
                |
                |  val f = { a: Int -> a.toString() }
                |  val f2 = fun (a: Int) = a + a
                |  val f3 = Runnable { a }
                |}
                """
                .trimMargin())

    assertThat(
            ktFile.toAElement().collectDescendantsOfType<ADeclarationOrLambdaWithBody>().map {
              it.text
            })
        .containsExactly(
            "()",
            "fun doIt() {}",
            "get() { return 7 }",
            "{ a: Int -> a.toString() }",
            "fun (a: Int) = a + a",
            "{ a }",
        )
  }
}
