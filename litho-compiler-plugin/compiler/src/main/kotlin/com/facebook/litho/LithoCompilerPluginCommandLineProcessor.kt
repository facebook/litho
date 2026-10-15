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

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Command-line processor for the Litho compiler plugin.
 *
 * The plugin has no options. Anything a feature needs to be told is read from a resource on the
 * plugin's own classpath instead, which keeps the option map the build passes empty and identical
 * for every target.
 *
 * `processOption` is left to the interface. It cannot be reached -- the compiler rejects an option
 * [pluginOptions] does not declare before routing one here -- and overriding it buys nothing:
 * Kotlin emits a bridge to the interface default either way, so `ANDROIDLINT.DeprecatedMethod`
 * reports this class whatever shape it takes, and neither `@Suppress("DEPRECATION")` nor
 * `@Suppress("DeprecatedMethod")` silences it. The same warning applies to every implementor of
 * [CommandLineProcessor], which is the only way to declare a compiler plugin's option surface.
 *
 * @see [LithoCompilerPluginRegistrar]
 */
internal const val LITHO_COMPILER_PLUGIN_ID: String = "com.facebook.litho.compiler"

@OptIn(ExperimentalCompilerApi::class)
class LithoCompilerPluginCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = LITHO_COMPILER_PLUGIN_ID
  override val pluginOptions: Collection<AbstractCliOption> = emptyList()
}
