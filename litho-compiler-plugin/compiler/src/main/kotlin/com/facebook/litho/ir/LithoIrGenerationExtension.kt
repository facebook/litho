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

package com.facebook.litho.ir

import com.facebook.litho.LithoCompilerPlugin
import com.facebook.litho.LithoCompilerPlugins
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * Runs the IR transform of every [LithoCompilerPlugin] that has one, in registration order.
 *
 * A single extension rather than one per plugin: `META-INF/services` decides the order of
 * separately registered extensions implicitly, and a transform has to be able to rely on the shape
 * of the IR it is handed.
 *
 * A throwing transform is left to propagate and fail the compilation. Transforms rewrite the module
 * in place, so catching one would hand codegen a half-rewritten module and emit it: for Auto Deps,
 * the call sites it had not reached yet still have no `@Deps` argument, and a later lowering fills
 * each one with the parameter's default empty array. Failing the build is the only outcome that
 * does not ship a silently empty dependency list.
 */
class LithoIrGenerationExtension(
    private val messages: MessageCollector,
    private val plugins: List<LithoCompilerPlugin> = LithoCompilerPlugins.ALL,
) : IrGenerationExtension {

  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    for (plugin in plugins) {
      val transformer = plugin.irTransform(pluginContext, messages) ?: continue
      moduleFragment.transform(transformer, null)
    }
  }
}
