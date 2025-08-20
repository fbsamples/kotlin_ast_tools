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

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.Test

/** Tests [PsiActions] */
class PsiActionsTest {

  @Test
  fun `replace an integer constant with +1 using a list`() {
    val ktFile =
        KotlinParserUtil.parseAsFile(
            """
          |fun f() {
          |  doIt(1)
          |  doIt(2)
          |  val a = 1
          |}
        """
                .trimMargin()
        )

    val nodes = ktFile.collectDescendantsOfType<KtExpression> { it.text?.toIntOrNull() != null }
    val replacements = nodes.map { (it.text.toInt() + 1).toString() }
    val newCode = replaceElements(ktFile.text, nodes, replacements)

    assertThat(newCode)
        .isEqualTo(
            """
          |fun f() {
          |  doIt(2)
          |  doIt(3)
          |  val a = 2
          |}
        """
                .trimMargin()
        )
  }
}
