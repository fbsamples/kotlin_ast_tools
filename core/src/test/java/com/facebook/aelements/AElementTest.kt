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
import org.jetbrains.kotlin.com.intellij.psi.PsiBinaryExpression
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.junit.Test

/** Tests [AElement] */
class AElementTest {

  @Test
  fun `basic functionality`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.example.foo;
        |
        |import com.example.bar.Bar;
        |
        |public class TestClass {
        |  public void doIt() {
        |    Bar bar = new Bar();
        |    if (1 + 1 == 2) {
        |      System.out.println("Hello World");
        |    }
        |  }
        |}
        """
                .trimMargin()
        )
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.example.foo
        |
        |import com.example.bar.Bar
        |
        |class TestClass {
        |  fun doIt() {
        |    val bar = Bar()
        |    if (1 + 1 == 2) {
        |     println("Hello World")
        |    }
        |  }
        |}
        """
                .trimMargin()
        )

    val javaElements = mutableListOf<PsiElement>()
    val kotlinElements = mutableListOf<PsiElement>()
    for (aFile in listOf(AFile(ktFile), AFile(javaPsiFile))) {
      val expressions: List<AExpressionOrStatement> =
          aFile.collectDescendantsOfType<AExpressionOrStatement>()
      for (expression in expressions) {
        when (expression) {
          is ABinaryExpression -> {
            if (expression.operator != "+") {
              expression.ifLanguage(
                  isJava = { it: PsiBinaryExpression -> javaElements += it },
                  isKotlin = { it: KtBinaryExpression -> kotlinElements += it },
              )
            }
          }
          is AIfExpressionOrStatement -> {
            expression.condition.ifLanguage(
                isJava = { javaElements += it },
                isKotlin = { kotlinElements += it },
            )
          }
        }
      }
    }
    assertThat(javaElements.map { it.text }).containsExactly("1 + 1 == 2", "1 + 1 == 2")
    assertThat(kotlinElements.map { it.text }).containsExactly("1 + 1 == 2", "1 + 1 == 2")
  }

  @Test
  fun `test getParentOfType`() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.example.foo;
        |
        |import com.example.bar.Bar;
        |
        |public class TestClass {
        |  public class TestClass2 {
        |    public class TestClass3 {
        |      public void doIt() {}
        |    }
        |  }
        |}
        """
                .trimMargin()
        )
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.example.foo
        |
        |import com.example.bar.Bar
        |
        |class TestClass {
        |  class TestClass2 {
        |    class TestClass3 {
        |      fun doIt() {}
        |    }
        |  }
        |}
        """
                .trimMargin()
        )

    for (aFile in listOf(ktFile.toAElement(), javaPsiFile.toAElement())) {
      val aClassOrObject = aFile.findDescendantOfType<AClassOrObject> { it.name == "TestClass3" }!!
      assertThat(aClassOrObject.getParentOfType<AClassOrObject>()!!.name).isEqualTo("TestClass2")
      assertThat(aClassOrObject.getParentOfType<AClassOrObject>(strict = true)!!.name)
          .isEqualTo("TestClass2")
      assertThat(aClassOrObject.getParentOfType<AClassOrObject>(strict = false)!!.name)
          .isEqualTo("TestClass3")

      assertThat(aClassOrObject.parentsOfType<AClassOrObject>().map { it.name }.toList())
          .containsExactly("TestClass2", "TestClass")
      assertThat(
              aClassOrObject.parentsOfType<AClassOrObject>(withSelf = true).map { it.name }.toList()
          )
          .containsExactly("TestClass3", "TestClass2", "TestClass")
    }
  }

  @Test
  fun testGetLineNumber() {
    val javaPsiFile =
        JavaPsiParserUtil.parseAsFile(
            """
        |package com.example.foo;
        |
        |import com.example.bar.Bar;
        |
        |public class TestClass {
        |  public class TestClass2 {
        |    public class TestClass3 {
        |      public void doIt() {}
        |    }
        |  }
        |  
        |  protected <T extends Integer> T createInteger() {
        |    return null;
        |  }
        |}
        """
                .trimMargin()
        )
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.example.foo
        |
        |import com.example.bar.Bar
        |
        |class TestClass {
        |  class TestClass2 {
        |    class TestClass3 {
        |      fun doIt() {}
        |    }
        |  }
        |}
        """
                .trimMargin()
        )
    assertThat(ktFile.toAElement().findDescendantOfType<ANamedFunction>()?.lineNumberInFile)
        .isEqualTo(8)
    assertThat(javaPsiFile.toAElement().findDescendantOfType<ANamedFunction>()?.lineNumberInFile)
        .isEqualTo(8)
    assertThat(javaPsiFile.toAElement().collectDescendantsOfType<AClassOrObject>().size)
        .isEqualTo(3)
  }
}
