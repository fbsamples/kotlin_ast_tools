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

/** Tests [AForExpression] for Kotlin for loops */
class AForExpressionTest {

  @Test
  fun `test Kotlin for loop with collection`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt(items: List<String>) {
            |    for (item in items) {
            |      println(item)
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForExpression>()!!

    assertThat(forLoop.loopParameter).isNotNull()
    assertThat(forLoop.loopParameter?.text).isEqualTo("item")
    assertThat(forLoop.loopRange?.text).isEqualTo("items")
    assertThat(forLoop.body).isNotNull()
    assertThat(forLoop.body?.text?.trim()).isEqualTo("{\n      println(item)\n    }")
  }

  @Test
  fun `test Kotlin for loop with range`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    for (i in 0..9) {
            |      println(i)
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForExpression>()!!

    assertThat(forLoop.loopParameter).isNotNull()
    assertThat(forLoop.loopParameter?.text).isEqualTo("i")
    assertThat(forLoop.loopRange?.text).isEqualTo("0..9")
    assertThat(forLoop.body).isNotNull()
  }

  @Test
  fun `test Kotlin for loop with step`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt() {
            |    for (i in 0 until 10 step 2) {
            |      println(i)
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForExpression>()!!

    assertThat(forLoop.loopParameter?.text).isEqualTo("i")
    assertThat(forLoop.loopRange?.text).isEqualTo("0 until 10 step 2")
  }

  @Test
  fun `test Kotlin for loop with destructuring`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt(pairs: List<Pair<String, Int>>) {
            |    for ((name, value) in pairs) {
            |      println("${'$'}name: ${'$'}value")
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForExpression>()!!

    assertThat(forLoop.loopParameter).isNotNull()
    assertThat(forLoop.loopParameter?.text).isEqualTo("(name, value)")
    assertThat(forLoop.loopRange?.text).isEqualTo("pairs")
  }

  @Test
  fun `test Kotlin for loop with single expression body`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt(items: List<String>) {
            |    for (item in items) println(item)
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForExpression>()!!

    assertThat(forLoop.body).isNotNull()
    assertThat(forLoop.body?.text).isEqualTo("println(item)")
  }
}
