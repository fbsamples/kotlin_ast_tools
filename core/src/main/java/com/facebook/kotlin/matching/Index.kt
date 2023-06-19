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

package com.facebook.kotlin.matching

/**
 * A representation of a an index instruction for matchers on lists
 *
 * When trying to findAll a specific argument of a method call for example, you can talk about the
 * argument using Index.at(2), or Index.last()
 */
class Index(private val index: Int) {
  fun isValid(collection: Collection<*>): Boolean {
    return when {
      collection.isEmpty() -> false
      index < 0 -> true
      else -> index < collection.size
    }
  }

  fun <T> getValue(collection: List<T>): List<T> {
    check(isValid(collection))
    return when (index) {
      ANY -> collection
      FIRST -> listOf(collection.first())
      LAST -> listOf(collection.last())
      else -> listOf(collection[index])
    }
  }

  companion object {
    const val FIRST = -1
    const val LAST = -2
    const val ANY = -3

    fun at(index: Int): Index {
      check(index >= 0)
      return Index(index)
    }

    fun first(): Index {
      return Index(FIRST)
    }

    fun last(): Index {
      return Index(LAST)
    }

    fun any(): Index {
      return Index(ANY)
    }
  }
}
