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

package com.facebook.matching

import com.facebook.asttools.KotlinParserUtil
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtCallExpression
import org.junit.Test

/** Tests [PsiAstMatcherImpl] */
class PsiAstMatcherImplTest {

  @Test
  fun `test matcher reorders matching functions`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
        |package com.test
        |
        |fun doIt() {
        |  doIt(5)
        |  doNotDoIt(6)
        |}"""
                .trimMargin()
        )
    var counter1 = 0
    var counter2 = 0
    val results =
        match<KtCallExpression>()
            .apply {
              addChildMatcher {
                counter1++
                Thread.sleep(1)
                "doIt" in it.text
              }
              addChildMatcher {
                counter2++
                "doIt" in it.text
              }
            }
            .findAll(ktFile)

    assertThat(results.map { it.text }).containsExactly("doIt(5)")
    assertThat(counter1).isEqualTo(1)
    assertThat(counter2).isEqualTo(2)
  }
}
