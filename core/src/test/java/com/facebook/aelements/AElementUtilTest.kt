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
import org.jetbrains.kotlin.com.intellij.psi.PsiJvmModifiersOwner
import org.jetbrains.kotlin.com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.psi.KtAnnotated
import org.junit.Test

/** Tests [AElementUtil] */
class AElementUtilTest {
  @Test
  fun `test AElement conversion for fields`() {
    val javaField = JavaPsiParserUtil.parseAsField("int n = 5;")
    assertThat(javaField.toAElement()).isInstanceOf(AProperty::class.java)
    assertThat((javaField as PsiJvmModifiersOwner).toAElement()).isInstanceOf(AProperty::class.java)

    val kotlinProperty = KotlinParserUtil.parseAsProperty("val n: Int = 5")
    assertThat(kotlinProperty.toAElement()).isInstanceOf(AProperty::class.java)
    assertThat((kotlinProperty as KtAnnotated).toAElement()).isInstanceOf(AProperty::class.java)
  }

  @Test
  fun `test AElement conversion for expressions`() {
    assertThat((JavaPsiParserUtil.parseAsExpression("1 + 1")).toAElement())
        .isInstanceOf(ABinaryExpression::class.java)
    assertThat((JavaPsiParserUtil.parseAsExpression("a.b()")).toAElement())
        .isInstanceOf(AQualifiedCallExpression::class.java)
    assertThat(
            (JavaPsiParserUtil.parseAsExpression("a.b()") as PsiMethodCallExpression).toAElement()
        )
        .isInstanceOf(AQualifiedCallExpression::class.java)
    assertThat((JavaPsiParserUtil.parseAsExpression("b()")).toAElement())
        .isInstanceOf(ACallExpression::class.java)

    assertThat((KotlinParserUtil.parseAsExpression("1 + 1")).toAElement())
        .isInstanceOf(ABinaryExpression::class.java)
    assertThat((KotlinParserUtil.parseAsExpression("a.b()")).toAElement())
        .isInstanceOf(AQualifiedCallExpression::class.java)
    assertThat((KotlinParserUtil.parseAsExpression("b()")).toAElement())
        .isInstanceOf(ACallExpression::class.java)
    assertThat((KotlinParserUtil.parseAsExpression("if (true) 1 else 2")).toAElement())
        .isInstanceOf(AIfExpressionOrStatement::class.java)
    assertThat((JavaPsiParserUtil.parseAsExpression("new Foo()")).toAElement())
        .isInstanceOf(AMethodOrNewCallExpression::class.java)
  }
}
