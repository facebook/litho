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
import com.facebook.litho.k2.LithoFirCheckersExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.FirSession

/**
 * Gives every `@Hook` call a stable structural identity, and enforces the one rule that identity
 * cannot make safe: a hook is still only callable from `render()` or another `@Hook` function,
 * because that is where the identity it nests under comes from.
 *
 * @see [HookIdentityTransformer]
 * @see [LithoFirCheckersExtension]
 */
object HookIdentityPlugin : LithoCompilerPlugin {

  override fun irTransform(
      pluginContext: IrPluginContext,
      messages: MessageCollector,
  ): HookIdentityTransformer = HookIdentityTransformer(pluginContext)

  override val firCheckers: (FirSession) -> LithoFirCheckersExtension
    get() = ::LithoFirCheckersExtension
}
