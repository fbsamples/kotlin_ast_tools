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

/** Tests [AForStatement] for Java traditional for loops */
class AForStatementTest {

  @Test
  fun `test traditional Java for loop`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |public class TestClass {
            |  public void doIt() {
            |    for (int i = 0; i < 10; i++) {
            |      System.out.println(i);
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaPsiFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForStatement>()!!

    assertThat(forLoop.initialization?.text).isEqualTo("int i = 0;")
    assertThat(forLoop.condition?.text).isEqualTo("i < 10")
    assertThat(forLoop.update?.text).isEqualTo("i++")
    assertThat(forLoop.body?.text?.trim()).isEqualTo("{\n      System.out.println(i);\n    }")
  }

  @Test
  fun `test for loop with multiple initialization and updates`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |public class TestClass {
            |  public void doIt() {
            |    for (int i = 0, j = 10; i < j; i++, j--) {
            |      System.out.println(i + j);
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaPsiFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForStatement>()!!

    assertThat(forLoop.initialization?.text).isEqualTo("int i = 0, j = 10;")
    assertThat(forLoop.condition?.text).isEqualTo("i < j")
    assertThat(forLoop.update?.text).isEqualTo("i++, j--")
  }

  @Test
  fun `test for loop with empty sections`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |public class TestClass {
            |  public void doIt() {
            |    int i = 0;
            |    for (; i < 10; ) {
            |      i++;
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaPsiFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForStatement>()!!

    // Empty sections in Java for loops return null
    assertThat(forLoop.initialization).isNull()
    assertThat(forLoop.condition?.text).isEqualTo("i < 10")
    assertThat(forLoop.update).isNull()
  }

  @Test
  fun `test for loop with single statement body`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |public class TestClass {
            |  public void doIt() {
            |    for (int i = 0; i < 10; i++)
            |      System.out.println(i);
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaPsiFile.toAElement()
    val forLoop = aFile.findDescendantOfType<AForStatement>()!!

    assertThat(forLoop.body?.text).isEqualTo("System.out.println(i);")
  }
}
