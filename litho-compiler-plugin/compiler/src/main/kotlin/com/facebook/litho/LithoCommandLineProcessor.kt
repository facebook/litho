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
import com.facebook.litho.common.LithoCompilerConfig.Companion.DEBUG
import com.facebook.litho.common.LithoCompilerConfig.Companion.ENABLED
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Entry point through which options from the command line are processed and passed to the compiler
 * plugin.
 *
 * Allowed options are defined in [LithoCompilerConfig].
 *
 * The options are passed to the compiler in the form of a list of strings, where each string is in
 * the form of `-Xplugin:$pluginId:$optionName=$optionValue`. For example:
 * `-Xplugin:com.facebook.litho.compiler:enabled=true`
 *
 * When using BUCK, this is equivalent to:
 * ```
 * kotlin_compiler_plugins = {
 *   "//fbandroid/libraries/components/litho-compiler-plugin:litho-compiler-plugin": {
 *      "plugin:com.facebook.litho.compiler:enabled": "true",
 *   },
 * },
 * ```
 *
 * The options are then processed by this class and stored in the [CompilerConfiguration] object.
 * This object is what gets passed to the compiler plugin, which can then use the options to
 * configure its behavior.
 *
 * @see [LithoCompilerConfig]
 * @see [LithoComponentRegistrar]
 */
@OptIn(ExperimentalCompilerApi::class)
class LithoCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = "com.facebook.litho.compiler"
  override val pluginOptions: Collection<AbstractCliOption> = LithoCompilerConfig.options

  override fun processOption(
      option: AbstractCliOption,
      value: String,
      configuration: CompilerConfiguration
  ) {
    when (option) {
      ENABLED.cliOption -> configuration.put(ENABLED.configKey, value.toBoolean())
      DEBUG.cliOption -> configuration.put(DEBUG.configKey, value.toBoolean())
      else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
  }
}
