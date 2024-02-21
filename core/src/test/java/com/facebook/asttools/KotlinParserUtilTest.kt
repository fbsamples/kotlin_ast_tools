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

package com.facebook.asttools

import java.lang.IllegalStateException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.junit.Test

/** Tests [KotlinParserUtil] */
class KotlinParserUtilTest {

  @Test
  fun `parse as file returns a KtFile with correct class name`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
      |package com.facebook.foo
      |
      |class Foo {
      |  val a = 1
      |}
    """
                .trimMargin())

    val classOrObject = ktFile.findChildByClass(KtClassOrObject::class.java)
    assertThat(classOrObject).isNotNull()
    assertThat(classOrObject?.name).isEqualTo("Foo")
  }

  @Test
  fun `parse as KtProperty`() {
    val ktProperty = KotlinParserUtil.parseAsProperty("""val a = 1""")

    assertThat(ktProperty.name).isEqualTo("a")
    assertThat(ktProperty.text).isEqualTo("val a = 1")
  }

  @Test
  fun `parse as KtExpression`() {
    val ktExpression = KotlinParserUtil.parseAsExpression("""doIt(1 + 1)""")

    assertThat(ktExpression.text).isEqualTo("doIt(1 + 1)")
  }

  @Test
  fun `parse as KtAnnotationEntry`() {
    val ktAnnotationEntry = KotlinParserUtil.parseAsAnnotationEntry("""@Magic""")
    assertThat(ktAnnotationEntry.text).isEqualTo("@Magic")

    val ktAnnotationEntryWithArgs = KotlinParserUtil.parseAsAnnotationEntry("""@Magic(value = 1)""")
    assertThat(ktAnnotationEntryWithArgs.text).isEqualTo("@Magic(value = 1)")
  }

  @Test
  fun `parse as KtParameter`() {
    val simpleKtParameter = KotlinParserUtil.parseAsParameter("""foo: Foo""")
    assertThat(simpleKtParameter.text).isEqualTo("foo: Foo")

    val ktParameter = KotlinParserUtil.parseAsParameter("""@Magic private val foo: Foo""")
    assertThat(ktParameter.text).isEqualTo("@Magic private val foo: Foo")
    assertThat(ktParameter.name).isEqualTo("foo")
    assertThat(ktParameter.typeReference?.text).isEqualTo("Foo")
    assertThat(ktParameter.annotationEntries.single().text).isEqualTo("@Magic")
    assertThat(ktParameter.valOrVarKeyword?.text).isEqualTo("val")
  }

  @Test
  fun `parse as KtDeclaration`() {
    assertThat(KotlinParserUtil.parseAsDeclaration("fun foo() = 1").text).isEqualTo("fun foo() = 1")
    assertThat(KotlinParserUtil.parseAsDeclaration("val foo = 1").text).isEqualTo("val foo = 1")
    assertThat(KotlinParserUtil.parseAsDeclaration("get() = 1").text).isEqualTo("get() = 1")
  }

  @Test
  fun `when cannot parse give a good error message`() {
    try {
      KotlinParserUtil.parseAsParameter("""@Hello""")
      fail("Expected exception")
    } catch (e: IllegalStateException) {
      assertThat(e).hasMessage("Cannot parse as parameter: '@Hello'")
    }
  }
}
