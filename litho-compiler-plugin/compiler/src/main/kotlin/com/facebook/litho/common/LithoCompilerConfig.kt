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

package com.facebook.litho.common

import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

class LithoCompilerConfig<T> private constructor(val cliOption: CliOption, val default: T?) {
  init {
    if (default == null) require(cliOption.required)
  }

  val configKey: CompilerConfigurationKey<T> = CompilerConfigurationKey.create(cliOption.optionName)

  companion object {
    val ENABLED: LithoCompilerConfig<Boolean> =
        LithoCompilerConfig(
            CliOption("enabled", "<true | false>", "whether plugin is enabled", false),
            default = false)
    internal val DEBUG: LithoCompilerConfig<Boolean> =
        LithoCompilerConfig(
            CliOption("internal.debug", "<true | false>", "whether debug mode is enabled", false),
            default = false)

    val options: List<CliOption>
      get() = listOf(ENABLED.cliOption, DEBUG.cliOption)
  }
}

operator fun <T> CompilerConfiguration.get(property: LithoCompilerConfig<T>): T {
  return when (property.default) {
    null -> getNotNull(property.configKey)
    else -> get(property.configKey, property.default)
  }
}
