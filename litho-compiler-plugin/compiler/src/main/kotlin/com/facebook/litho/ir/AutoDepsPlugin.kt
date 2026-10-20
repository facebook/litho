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
import com.facebook.litho.common.AutoDepsConfig
import com.facebook.litho.k2.AutoDepsCheckersExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.FirSession

/**
 * Infers the dependency list of an `@AutoDeps` call from what its lambda captures.
 *
 * [config] carries the type lists the analysis cannot derive -- it defaults to the one shipped as a
 * jar resource, and a test supplies its own by registering a differently configured instance.
 *
 * @see [AutoDepsTransformer]
 * @see [AutoDepsCheckersExtension]
 */
class AutoDepsPlugin(private val config: AutoDepsConfig = AutoDepsConfig.load()) :
    LithoCompilerPlugin {

  override fun irTransform(
      pluginContext: IrPluginContext,
      messages: MessageCollector,
  ): AutoDepsTransformer = AutoDepsTransformer(pluginContext, config, messages)

  override val firCheckers: (FirSession) -> AutoDepsCheckersExtension
    get() = ::AutoDepsCheckersExtension
}
