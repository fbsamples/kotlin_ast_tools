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

/** Tests [ADoWhileExpression] for Kotlin do-while loops */
class ADoWhileExpressionTest {

  @Test
  fun `test Kotlin do-while loop with simple condition`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    var i = 0
            |    do {
            |      println(i)
            |      i++
            |    } while (i < 10)
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val doWhileLoop = aFile.findDescendantOfType<ADoWhileExpression>()!!

    assertThat(doWhileLoop.condition).isNotNull()
    assertThat(doWhileLoop.condition?.text).isEqualTo("i < 10")
    assertThat(doWhileLoop.body).isNotNull()
  }

  @Test
  fun `test Kotlin do-while loop with complex condition`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    var i = 0
            |    var j = 10
            |    do {
            |      i++
            |    } while (i < j && i < 100)
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val doWhileLoop = aFile.findDescendantOfType<ADoWhileExpression>()!!

    assertThat(doWhileLoop.condition).isNotNull()
    assertThat(doWhileLoop.condition?.text).isEqualTo("i < j && i < 100")
    assertThat(doWhileLoop.body).isNotNull()
  }

  @Test
  fun `test Kotlin do-while loop with single statement body`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    var i = 0
            |    do
            |      i++
            |    while (i < 10)
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val doWhileLoop = aFile.findDescendantOfType<ADoWhileExpression>()!!

    assertThat(doWhileLoop.condition).isNotNull()
    assertThat(doWhileLoop.condition?.text).isEqualTo("i < 10")
    assertThat(doWhileLoop.body).isNotNull()
    assertThat(doWhileLoop.body?.text?.trim()).isEqualTo("i++")
  }

  @Test
  fun `test Kotlin do-while loop with initially false condition`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    var i = 100
            |    do {
            |      i++
            |    } while (i < 10)
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val doWhileLoop = aFile.findDescendantOfType<ADoWhileExpression>()!!

    assertThat(doWhileLoop.condition).isNotNull()
    assertThat(doWhileLoop.condition?.text).isEqualTo("i < 10")
    assertThat(doWhileLoop.body).isNotNull()
  }
}
