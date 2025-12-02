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

/** Tests [AWhenExpression] for Kotlin when expressions */
class AWhenExpressionTest {

  @Test
  fun `test Kotlin when with subject expression`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt(x: Int) {
            |    when (x) {
            |      1 -> println("One")
            |      2 -> println("Two")
            |      else -> println("Other")
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val whenExpr = aFile.findDescendantOfType<AWhenExpression>()!!

    assertThat(whenExpr.subjectExpression).isNotNull()
    assertThat(whenExpr.subjectExpression?.text).isEqualTo("x")
    assertThat(whenExpr.entries).isNotEmpty
  }

  @Test
  fun `test Kotlin when without subject`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt(x: Int) {
            |    when {
            |      x < 0 -> println("Negative")
            |      x > 0 -> println("Positive")
            |      else -> println("Zero")
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val whenExpr = aFile.findDescendantOfType<AWhenExpression>()!!

    assertThat(whenExpr.subjectExpression).isNull()
    assertThat(whenExpr.entries).isNotEmpty
  }

  @Test
  fun `test Kotlin when with multiple conditions per entry`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt(day: Int) {
            |    when (day) {
            |      1, 2, 3, 4, 5 -> println("Weekday")
            |      6, 7 -> println("Weekend")
            |      else -> println("Invalid")
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val whenExpr = aFile.findDescendantOfType<AWhenExpression>()!!

    assertThat(whenExpr.subjectExpression).isNotNull()
    assertThat(whenExpr.subjectExpression?.text).isEqualTo("day")
    assertThat(whenExpr.entries).isNotEmpty
  }

  @Test
  fun `test Kotlin when with else branch`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
            |package com.example.foo
            |
            |class TestClass {
            |  fun doIt(s: String) {
            |    val result = when (s.toLowerCase()) {
            |      "a" -> "Letter A"
            |      "b" -> "Letter B"
            |      else -> "Unknown"
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = ktFile.toAElement()
    val whenExpr = aFile.findDescendantOfType<AWhenExpression>()!!

    assertThat(whenExpr.subjectExpression).isNotNull()
    assertThat(whenExpr.subjectExpression?.text).contains("toLowerCase")
    assertThat(whenExpr.entries).isNotEmpty
  }
}
