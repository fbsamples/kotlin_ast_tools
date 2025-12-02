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

/** Tests [AForeachStatement] for Java enhanced for loops */
class AForeachStatementTest {

  @Test
  fun `test Java foreach loop with list`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |import java.util.List;
            |
            |public class TestClass {
            |  public void doIt(List<String> items) {
            |    for (String item : items) {
            |      System.out.println(item);
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaPsiFile.toAElement()
    val foreachLoop = aFile.findDescendantOfType<AForeachStatement>()!!

    assertThat(foreachLoop.iterationParameter?.text).isEqualTo("String item")
    assertThat(foreachLoop.iteratedValue?.text).isEqualTo("items")
    assertThat(foreachLoop.body?.text?.trim())
        .isEqualTo("{\n      System.out.println(item);\n    }")
  }

  @Test
  fun `test Java foreach loop with array`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |public class TestClass {
            |  public void doIt(int[] numbers) {
            |    for (int num : numbers) {
            |      System.out.println(num);
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaPsiFile.toAElement()
    val foreachLoop = aFile.findDescendantOfType<AForeachStatement>()!!

    assertThat(foreachLoop.iterationParameter?.text).isEqualTo("int num")
    assertThat(foreachLoop.iteratedValue?.text).isEqualTo("numbers")
  }

  @Test
  fun `test Java foreach loop with single statement body`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |public class TestClass {
            |  public void doIt(String[] items) {
            |    for (String item : items)
            |      System.out.println(item);
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaPsiFile.toAElement()
    val foreachLoop = aFile.findDescendantOfType<AForeachStatement>()!!

    assertThat(foreachLoop.iterationParameter?.text).isEqualTo("String item")
    assertThat(foreachLoop.iteratedValue?.text).isEqualTo("items")
    assertThat(foreachLoop.body?.text).isEqualTo("System.out.println(item);")
  }

  @Test
  fun `test Java foreach loop with final modifier`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
            |package com.example.foo;
            |
            |import java.util.List;
            |
            |public class TestClass {
            |  public void doIt(List<String> items) {
            |    for (final String item : items) {
            |      System.out.println(item);
            |    }
            |  }
            |}
            """
                .trimMargin()
        )

    val aFile = javaPsiFile.toAElement()
    val foreachLoop = aFile.findDescendantOfType<AForeachStatement>()!!

    assertThat(foreachLoop.iterationParameter?.text).isEqualTo("final String item")
  }
}
