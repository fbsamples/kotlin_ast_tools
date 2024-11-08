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
import org.junit.Test

/** Tests [AModifierListOwner] */
class AModifierListOwnerTest {

  @Test
  fun `basic functionality`() {
    val (javaElement, kotlinElement) =
        JavaPsiParserUtil.parseAsFile(
                """
                |public class TestClass {
                |  private int aPrivate = 1;
                |  protected int aProtected = 1;
                |  int aPackage = 1;
                |  public int aPublic = 1;
                |
                |  interface I {
                |    int aPublic = 1;
                |  }
                |}
                """
                    .trimMargin())
            .toAElement() to
            KotlinParserUtil.parseAsFile(
                    """
                |open class TestClass {
                |  private val aPrivate = 1
                |  protected val aProtected = 1
                |  internal val aInternal = 1
                |  val aPublic = 1
                |  public val aPublic2 = 1
                |}
                """
                        .trimMargin())
                .toAElement()

    for (aFile in listOf(javaElement, kotlinElement)) {
      assertThat(
              aFile
                  .collectDescendantsOfType<AProperty> { it.name!!.contains("Public") }
                  .map { it.isPublic }
                  .distinct())
          .containsExactly(true)
      assertThat(
              aFile
                  .collectDescendantsOfType<AProperty> { it.name!!.contains("Private") }
                  .map { it.isPrivate }
                  .distinct())
          .containsExactly(true)
      assertThat(
              aFile
                  .collectDescendantsOfType<AProperty> { it.name!!.contains("Internal") }
                  .map { it.isInternal }
                  .distinct())
          .doesNotContain(false)
      assertThat(
              aFile
                  .collectDescendantsOfType<AProperty> { it.name!!.contains("Package") }
                  .map { it.isPackage }
                  .distinct())
          .doesNotContain(false)
      assertThat(
              aFile
                  .collectDescendantsOfType<AProperty> { it.name!!.contains("Protected") }
                  .map { it.isProtected }
                  .distinct())
          .containsExactly(true)
    }
  }
}
