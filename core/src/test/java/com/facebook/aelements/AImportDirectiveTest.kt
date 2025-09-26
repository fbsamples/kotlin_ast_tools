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

import com.facebook.aelements.util.AElementTestingUtil
import com.facebook.asttools.JavaPsiParserUtil
import com.facebook.asttools.KotlinParserUtil
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.PsiImportStatementBase
import org.jetbrains.kotlin.psi.KtImportDirective
import org.junit.Test

/** Tests [ABinaryExpression] */
class AImportDirectiveTest {

  @Test
  fun `basic functionality`() {
    val aElementsTestUtil =
        AElementTestingUtil<AImportDirective, PsiImportStatementBase, KtImportDirective>()

    val (javaAImportDirective, kotlinAImportDirective) =
        aElementsTestUtil.loadTestAElements<AImportDirective>(
            javaCode =
                """
                |import com.foo.Bar;
                |
                |public class TestClass {}
                """
                    .trimMargin(),
            kotlinCode =
                """
                |import com.foo.Bar
                |
                |class TestClass
                """
                    .trimMargin(),
        )

    assertThat(javaAImportDirective.psiElement.toAElement())
        .isInstanceOf(AImportDirective::class.java)
    for (aElement in listOf(javaAImportDirective, kotlinAImportDirective)) {
      aElementsTestUtil.assertSameString(
          aElement,
          { it.fullyQualifiedName },
          { it.importReference?.qualifiedName },
          { it.importedFqName?.asString() },
      )
    }
  }

  @Test
  fun `test loading static imports`() {
    val staticAElement =
        JavaPsiParserUtil.parseAsFile(
                """
                |import static com.foo.Bar.boom;
                |
                |import com.foo.Bar;
                |
                |public class TestClass {}
                """
                    .trimMargin()
            )
            .toAElement()
            .findDescendantOfType<AImportDirective> { it.text == "import static com.foo.Bar.boom;" }
    assertThat(staticAElement).isNotNull
    assertThat(staticAElement?.isStatic).isTrue
  }

  @Test
  fun `test loading aliases`() {
    val staticAElement =
        KotlinParserUtil.parseAsFile(
                """
                |import com.foo.Bar.boom as boomboom
                |
                |class TestClass
                """
                    .trimMargin()
            )
            .toAElement()
            .findDescendantOfType<AImportDirective> {
              it.text == "import com.foo.Bar.boom as boomboom"
            }
    assertThat(staticAElement).isNotNull
    assertThat(staticAElement?.alias).isEqualTo("boomboom")
  }

  @Test
  fun `test escaped names in Kotlin`() {
    assertThat(
            KotlinParserUtil.parseAsFile(
                    """
                    |import com.foo.Bar.`one two`
                    |
                    |class TestClass
                    """
                        .trimMargin()
                )
                .toAElement()
                .findDescendantOfType<AImportDirective> {
                  it.fullyQualifiedName == "com.foo.Bar.`one two`"
                }
        )
        .isNotNull()
  }
}
