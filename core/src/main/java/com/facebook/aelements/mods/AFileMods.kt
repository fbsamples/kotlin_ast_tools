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

package com.facebook.aelements.mods

import com.facebook.aelements.AElement
import com.facebook.aelements.AFile
import com.facebook.aelements.collectDescendantsOfType
import com.facebook.tools.codemods.writer.psi.PsiWriter

/**
 * Replaces all elements that match the given matcher with the given replacement in place. Add
 * necessary imports and buck dependencies
 */
inline fun <reified T : AElement> AFile.replaceInPlace(
    crossinline matcher: (T) -> Boolean,
    replacement: (T) -> String,
    imports: List<String> = emptyList(),
    deps: List<String> = emptyList(),
    psiWriter: PsiWriter,
) {
  val selections = collectDescendantsOfType<T> { matcher(it) }
  selections.forEach { selection -> psiWriter.changeTo(selection, replacement(selection)) }
  imports.forEach { psiWriter.addImport(it) }
  deps.forEach { psiWriter.buck.addDep(it) }
}
