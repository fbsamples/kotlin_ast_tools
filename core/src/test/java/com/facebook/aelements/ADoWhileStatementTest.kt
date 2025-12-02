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

/** Tests [ADoWhileStatement] for Java do-while loops */
class ADoWhileStatementTest {

  @Test
  fun `test Java do-while loop with simple condition`() {
    val javaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |class TestClass {
            |  public void doIt() {
            |    int i = 0;
            |    do {
            |      System.out.println(i);
            |      i++;
            |    } while (i < 10);
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaFile.toAElement()
    val doWhileLoop = aFile.findDescendantOfType<ADoWhileStatement>()!!

    assertThat(doWhileLoop.condition).isNotNull()
    assertThat(doWhileLoop.condition?.text).isEqualTo("i < 10")
    assertThat(doWhileLoop.body).isNotNull()
  }

  @Test
  fun `test Java do-while loop with complex condition`() {
    val javaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |class TestClass {
            |  public void doIt() {
            |    int i = 0;
            |    int j = 10;
            |    do {
            |      i++;
            |    } while (i < j && i < 100);
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaFile.toAElement()
    val doWhileLoop = aFile.findDescendantOfType<ADoWhileStatement>()!!

    assertThat(doWhileLoop.condition).isNotNull()
    assertThat(doWhileLoop.condition?.text).isEqualTo("i < j && i < 100")
    assertThat(doWhileLoop.body).isNotNull()
  }

  @Test
  fun `test Java do-while loop with single statement body`() {
    val javaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |class TestClass {
            |  public void doIt() {
            |    int i = 0;
            |    do
            |      i++;
            |    while (i < 10);
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaFile.toAElement()
    val doWhileLoop = aFile.findDescendantOfType<ADoWhileStatement>()!!

    assertThat(doWhileLoop.condition).isNotNull()
    assertThat(doWhileLoop.condition?.text).isEqualTo("i < 10")
    assertThat(doWhileLoop.body).isNotNull()
    assertThat(doWhileLoop.body?.text?.trim()).isEqualTo("i++;")
  }

  @Test
  fun `test Java do-while loop with initially false condition`() {
    val javaFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |class TestClass {
            |  public void doIt() {
            |    int i = 100;
            |    do {
            |      i++;
            |    } while (i < 10);
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaFile.toAElement()
    val doWhileLoop = aFile.findDescendantOfType<ADoWhileStatement>()!!

    assertThat(doWhileLoop.condition).isNotNull()
    assertThat(doWhileLoop.condition?.text).isEqualTo("i < 10")
    assertThat(doWhileLoop.body).isNotNull()
  }
}
