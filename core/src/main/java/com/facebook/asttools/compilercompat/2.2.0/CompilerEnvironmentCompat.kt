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

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

internal fun createCompilerConfigurationCompat(
    messageCollector: MessageCollector,
): CompilerConfiguration =
    CompilerConfiguration().apply {
      put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    }

internal fun createProjectCompat(
    configuration: CompilerConfiguration,
    disposable: Disposable,
    messageCollector: MessageCollector,
): Project = createProjectEnvironment(
    configuration,
    disposable,
    EnvironmentConfigFiles.JVM_CONFIG_FILES,
    messageCollector,
)
    .project
