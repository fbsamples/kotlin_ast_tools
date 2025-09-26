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

package com.facebook.kotlin.postconversion

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

/** Tests [PostConversionExamples] */
class PostConversionExamplesTest {

  private val root = createTempDirectory()

  @After
  fun tearDown() {
    root.toFile().deleteRecursively()
  }

  @Test
  fun `print no input files when no input is given`() {
    val output = ByteArrayOutputStream()
    PostConversionExamples.main(PrintStream(output), arrayOf())
    assertThat(output.toString()).contains("no input files")
  }

  @Test
  fun `replace TextUtils equals with ==`() {
    val file = createTempFile(directory = root, suffix = ".kt").toFile()
    file.writeText(
        """
        |package com.facebook.kotlin.examples
        |
        |import android.text.TextUtils
        |
        |object Foo {
        |  fun doIt(str1: String?, str2: String?) {
        |    println(true == TextUtils.equals(str1, str2))
        |    println(TextUtils.equals(str1, str2).toString())
        |    return TextUtils.equals(str1, str2)
        |  }
        |}
        |"""
            .trimMargin()
    )

    PostConversionExamples.main(System.out, arrayOf(file.path))

    assertThat(file.readText())
        .isEqualTo(
            """
            |package com.facebook.kotlin.examples
            |
            |import android.text.TextUtils
            |
            |object Foo {
            |  fun doIt(str1: String?, str2: String?) {
            |    println(true == (str1 == str2))
            |    println((str1 == str2).toString())
            |    return str1 == str2
            |  }
            |}
            |"""
                .trimMargin()
        )
  }

  @Test
  fun `replace TextUtils isEmpty with built in`() {
    val file = createTempFile(directory = root, suffix = ".kt").toFile()
    file.writeText(
        """
        |// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.
        |
        |package com.facebook.examples
        |
        |import android.text.TextUtils
        |
        |object Foo {
        |  fun doIt(str1: String?) {
        |    return TextUtils.isEmpty(str1)
        |  }
        |}
        |"""
            .trimMargin()
    )

    PostConversionExamples.main(System.out, arrayOf(file.path))

    assertThat(file.readText())
        .isEqualTo(
            """
            |// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.
            |
            |package com.facebook.examples
            |
            |import android.text.TextUtils
            |
            |object Foo {
            |  fun doIt(str1: String?) {
            |    return str1.isNullOrEmpty()
            |  }
            |}
            |"""
                .trimMargin()
        )
  }

  @Test
  fun `replace Guava Strings with built in functions`() {
    val file = createTempFile(directory = root, suffix = ".kt").toFile()
    file.writeText(
        """
        |// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.
        |
        |package com.facebook.examples
        |
        |import com.google.common.base.Strings
        |
        |object Foo {
        |  fun doIt(str: String?) {
        |    return Strings.isNullOrEmpty(str)
        |  }
        |
        |  fun doItAll(str: String?) {
        |   return Strings.nullToEmpty(str)
        |  }
        |}
        |"""
            .trimMargin()
    )

    PostConversionExamples.main(System.out, arrayOf(file.path))

    assertThat(file.readText())
        .isEqualTo(
            """
            |// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.
            |
            |package com.facebook.examples
            |
            |import com.google.common.base.Strings
            |
            |object Foo {
            |  fun doIt(str: String?) {
            |    return str.isNullOrEmpty()
            |  }
            |
            |  fun doItAll(str: String?) {
            |   return str.orEmpty()
            |  }
            |}
            |"""
                .trimMargin()
        )
  }

  @Test
  fun `when a var is used only for one possible assignment right after, replace with val`() {
    val file = createTempFile(directory = root, suffix = ".kt").toFile()
    file.writeText(
        """
        |package com.facebook.examples
        |
        |class Foo {
        |  fun foo(flag: Boolean) {
        |    var number = 1
        |    if (flag) {
        |      number = 2
        |    }
        |
        |    println(number)
        |  }
        |}
        |"""
            .trimMargin()
    )

    PostConversionExamples.main(System.out, arrayOf(file.path))

    assertThat(file.readText())
        .isEqualTo(
            """
            |package com.facebook.examples
            |
            |class Foo {
            |  fun foo(flag: Boolean) {
            |    val number = if (flag) 2 else 1
            |    
            |
            |    println(number)
            |  }
            |}
            |"""
                .trimMargin()
        )
  }
}
