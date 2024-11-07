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

package com.facebook.litho

import com.facebook.litho.common.LithoCompilerConfig
import com.facebook.litho.common.get
import com.facebook.litho.k1.LithoFeCheckersContributor
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension

@OptIn(ExperimentalCompilerApi::class)
class LithoComponentRegistrar : CompilerPluginRegistrar() {

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    if (!configuration[LithoCompilerConfig.ENABLED]) return
    val messageCollector =
        configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

    // K1 extensions
    StorageComponentContainerContributor.registerExtension(LithoFeCheckersContributor())
    if (configuration[LithoCompilerConfig.DEBUG]) {
      AnalysisHandlerExtension.registerExtension(
          LithoCodeAnalysisExtension { message, location ->
            messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
          })
    }
    // K2 extensions
    // Backend extensions
  }

  override val supportsK2: Boolean
    get() = true
}
