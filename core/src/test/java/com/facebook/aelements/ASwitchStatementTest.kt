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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Tests [ASwitchStatement] for Java switch statements */
class ASwitchStatementTest {

  @Test
  fun `test Java switch with multiple cases`() {
    val javaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |class TestClass {
            |  public void doIt(int value) {
            |    switch (value) {
            |      case 1:
            |        System.out.println("One");
            |        break;
            |      case 2:
            |        System.out.println("Two");
            |        break;
            |      default:
            |        System.out.println("Other");
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaFile.toAElement()
    val switchStmt = aFile.findDescendantOfType<ASwitchStatement>()!!

    assertThat(switchStmt.expression).isNotNull()
    assertThat(switchStmt.expression?.text).isEqualTo("value")
    assertThat(switchStmt.body).isNotNull()
  }

  @Test
  fun `test Java switch with default case`() {
    val javaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |class TestClass {
            |  public void doIt(String s) {
            |    switch (s) {
            |      case "hello":
            |        System.out.println("Hello!");
            |        break;
            |      default:
            |        System.out.println("Unknown");
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaFile.toAElement()
    val switchStmt = aFile.findDescendantOfType<ASwitchStatement>()!!

    assertThat(switchStmt.expression).isNotNull()
    assertThat(switchStmt.expression?.text).isEqualTo("s")
    assertThat(switchStmt.body).isNotNull()
  }

  @Test
  fun `test Java switch with fall-through cases`() {
    val javaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |class TestClass {
            |  public void doIt(int day) {
            |    switch (day) {
            |      case 1:
            |      case 2:
            |      case 3:
            |      case 4:
            |      case 5:
            |        System.out.println("Weekday");
            |        break;
            |      case 6:
            |      case 7:
            |        System.out.println("Weekend");
            |        break;
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaFile.toAElement()
    val switchStmt = aFile.findDescendantOfType<ASwitchStatement>()!!

    assertThat(switchStmt.expression).isNotNull()
    assertThat(switchStmt.expression?.text).isEqualTo("day")
    assertThat(switchStmt.body).isNotNull()
  }

  @Test
  fun `test Java switch with expression selector`() {
    val javaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |class TestClass {
            |  public void doIt(String s) {
            |    switch (s.toLowerCase()) {
            |      case "a":
            |        System.out.println("A");
            |        break;
            |      case "b":
            |        System.out.println("B");
            |        break;
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaFile.toAElement()
    val switchStmt = aFile.findDescendantOfType<ASwitchStatement>()!!

    assertThat(switchStmt.expression).isNotNull()
    assertThat(switchStmt.expression?.text).contains("toLowerCase")
    assertThat(switchStmt.body).isNotNull()
  }
}
