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
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.com.intellij.psi.PsiElement

/**
 * Utilities for easily asserting an AElement conforms to expected practices
 *
 * Implemeneted as class to reduce duplication of generic arguments.
 */
class AElementTestingUtil<T : AElement, J : PsiElement, K : PsiElement> {

  inline fun <reified T : AElement> loadTestAElements(
      @Language("java") javaCode: String,
      @Language("kotlin") kotlinCode: String
  ): Pair<T, T> {
    val javaAElement =
        JavaPsiParserUtil.parseAsFile(javaCode).toAElement().findDescendantOfType<T>()!!
    val kotlinAElement =
        KotlinParserUtil.parseAsFile(kotlinCode).toAElement().findDescendantOfType<T>()!!
    return Pair(javaAElement, kotlinAElement)
  }

  fun assertSamePsiElement(
      aElement: T,
      onAElement: (T) -> AElement?,
      onJava: (J) -> PsiElement?,
      onKotlin: (K) -> PsiElement?
  ) {
    val aElementResult = onAElement(aElement)?.psiElement
    val psiResult =
        aElement
            .ifLanguage(
                isJava = { onJava(aElement.psiElement as J) },
                isKotlin = { onKotlin(aElement.psiElement as K) })
            ?.toAElement()
            ?.psiElement
    assertThat(aElementResult)
        .withFailMessage(
            """
              |For language {${aElement.language}}, expected AElement result:
              |${aElementResult?.javaClass?.simpleName}:${aElementResult?.javaClass?.simpleName}, ${aElementResult?.text}
              |   to be equal to
              |${psiResult?.javaClass?.simpleName}:${psiResult?.javaClass?.simpleName}, ${psiResult?.text}"""
                .trimMargin())
        .isEqualTo(psiResult)
  }

  fun assertSamePsiElementList(
      aElement: T,
      onAElement: (T) -> List<AElement>?,
      onJava: (J) -> List<PsiElement?>?,
      onKotlin: (K) -> List<PsiElement?>?
  ) {
    val aElementResult = onAElement(aElement)?.map { it.psiElement }
    val psiResult =
        aElement.ifLanguage(
            isJava = { onJava(aElement.psiElement as J) },
            isKotlin = { onKotlin(aElement.psiElement as K) })
    assertThat(aElementResult)
        .withFailMessage(
            """
              |For language {${aElement.language}}, expected AElement result:
              |${aElementResult?.javaClass?.simpleName}:${aElementResult?.map { "${it.javaClass.simpleName}, ${it.text}"} }
              |   to be equal to
              |${psiResult?.javaClass?.simpleName}:${psiResult?.map { "${it?.javaClass?.simpleName}, ${it?.text}"} }"""
                .trimMargin())
        .isEqualTo(psiResult)
  }

  fun assertSameString(
      aElement: T,
      onAElement: (T) -> String?,
      onJava: (J) -> String?,
      onKotlin: (K) -> String?
  ) {
    val aElementResult = onAElement(aElement)
    val psiResult =
        aElement.ifLanguage(
            isJava = { onJava(aElement.psiElement as J) },
            isKotlin = { onKotlin(aElement.psiElement as K) })
    assertThat(aElementResult)
        .withFailMessage(
            """
              |For language {${aElement.language}}, expected AElement result:
              |${aElementResult?.javaClass?.simpleName}:${aElementResult}
              |   to be equal to
              |${psiResult?.javaClass?.simpleName}:${psiResult}"""
                .trimMargin())
        .isEqualTo(psiResult)
  }
}
