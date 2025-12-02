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

/** Tests [AWhileExpression] for Kotlin while loops */
class AWhileExpressionTest {

  @Test
  fun `test Kotlin while loop with simple condition`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    var i = 0
            |    while (i < 10) {
            |      println(i)
            |      i++
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val whileLoop = aFile.findDescendantOfType<AWhileExpression>()!!

    assertThat(whileLoop.condition).isNotNull()
    assertThat(whileLoop.condition?.text).isEqualTo("i < 10")
    assertThat(whileLoop.body).isNotNull()
  }

  @Test
  fun `test Kotlin while loop with complex condition`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    var i = 0
            |    var j = 10
            |    while (i < j && i < 100) {
            |      i++
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val whileLoop = aFile.findDescendantOfType<AWhileExpression>()!!

    assertThat(whileLoop.condition).isNotNull()
    assertThat(whileLoop.condition?.text).isEqualTo("i < j && i < 100")
    assertThat(whileLoop.body).isNotNull()
  }

  @Test
  fun `test Kotlin while loop with single statement body`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    var i = 0
            |    while (i < 10)
            |      i++
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val whileLoop = aFile.findDescendantOfType<AWhileExpression>()!!

    assertThat(whileLoop.condition).isNotNull()
    assertThat(whileLoop.condition?.text).isEqualTo("i < 10")
    assertThat(whileLoop.body).isNotNull()
    assertThat(whileLoop.body?.text?.trim()).isEqualTo("i++")
  }

  @Test
  fun `test Kotlin while loop with true condition`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    while (true) {
            |      break
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val whileLoop = aFile.findDescendantOfType<AWhileExpression>()!!

    assertThat(whileLoop.condition).isNotNull()
    assertThat(whileLoop.condition?.text).isEqualTo("true")
    assertThat(whileLoop.body).isNotNull()
  }
}
