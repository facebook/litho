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

import com.facebook.kotlin.compilercompat.CompilerPluginRegistrarCompat
import com.facebook.kotlin.compilercompat.registerDefaultDiagnosticsCompat
import com.facebook.litho.ir.LithoIrGenerationExtension
import com.facebook.litho.k2.LithoFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/**
 * The single entry point through which every [LithoCompilerPlugin] reaches the compiler.
 *
 * The plugin has no configuration: every compatible Kotlin target receives it, and each feature is
 * a no-op on a target that does not use it. Adding a feature means adding it to
 * [LithoCompilerPlugins.ALL]; this registrar and the build integration do not change.
 *
 * @see [LithoCompilerPluginCommandLineProcessor]
 */
@OptIn(ExperimentalCompilerApi::class)
class LithoCompilerPluginRegistrar(
    private val plugins: List<LithoCompilerPlugin> = LithoCompilerPlugins.ALL,
) : CompilerPluginRegistrarCompat() {

  override val pluginIdCompat: String = LITHO_COMPILER_PLUGIN_ID

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val messages = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    IrGenerationExtension.registerExtension(LithoIrGenerationExtension(messages, plugins))

    val checkers = plugins.mapNotNull { it.firCheckers }
    if (checkers.isNotEmpty()) {
      // A plugin diagnostic container registers itself with the renderer factory the moment it is
      // touched, and on Kotlin 2.2 that can happen before the compiler's own factory has been. The
      // `-Xwarning-level` names that `warnings_as_errors_k22` emits are validated against whatever
      // is registered by then, so an unprimed built-in factory fails the build with
      // `warning with name "DEPRECATION" does not exist` -- in a target that never mentions Litho.
      registerDefaultDiagnosticsCompat()
      FirExtensionRegistrarAdapter.registerExtension(LithoFirExtensionRegistrar(checkers))
    }
  }

  override val supportsK2: Boolean
    get() = true
}
