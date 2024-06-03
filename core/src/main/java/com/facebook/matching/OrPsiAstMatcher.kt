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

import com.intellij.psi.PsiElement

/**
 * Takes two matchers and merges them as one, this is needed to support one specific case for now so
 * we keep it as internal
 */
internal class OrPsiAstMatcher<
    CommonAncestorElement : PsiElement, E1 : CommonAncestorElement, E2 : CommonAncestorElement>(
    private val matcher1: PsiAstMatcher<E1>,
    private val matcher2: PsiAstMatcher<E2>,
) : PsiAstMatcher<CommonAncestorElement> {

  override val shouldMatchToNull: Boolean
    get() = matcher1.shouldMatchToNull || matcher2.shouldMatchToNull

  override fun matches(obj: PsiElement?): MatchResult<CommonAncestorElement?>? {
    return (matcher1.matches(obj) ?: matcher2.matches(obj)) as MatchResult<CommonAncestorElement?>?
  }
}
