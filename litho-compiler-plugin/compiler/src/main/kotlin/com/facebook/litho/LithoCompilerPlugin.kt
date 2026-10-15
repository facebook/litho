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

import com.facebook.litho.ir.HookIdentityPlugin
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * One compile-time feature of the Litho compiler plugin: an IR transform, a set of FIR checkers, or
 * both.
 *
 * [litho_hook_identity_decorator] installs this plugin's jar on every compatible Kotlin target with
 * an empty option map, so a feature is reached through the jar rather than through the build. A new
 * feature is therefore an entry in [LithoCompilerPlugins.ALL] and nothing else -- no plugin id of
 * its own, no CLI option, no BUCK, decorator or `META-INF/services` change.
 *
 * Neither member has a default. A plugin says what it does not contribute by returning null, so a
 * plugin that contributes nothing has to be written that way on purpose rather than reached by
 * forgetting an override -- which would register, run, and do nothing.
 *
 * @see [LithoCompilerPluginRegistrar]
 */
interface LithoCompilerPlugin {

  /** The transform to run over each module's IR, or null for a plugin that only checks. */
  fun irTransform(
      pluginContext: IrPluginContext,
      messages: MessageCollector,
  ): IrElementTransformerVoid?

  /** Builds this plugin's FIR checkers, or null for a plugin that only transforms. */
  val firCheckers: ((FirSession) -> FirAdditionalCheckersExtension)?
}

/** The one list every Litho compile-time feature is registered in. */
object LithoCompilerPlugins {

  /**
   * Every feature, in the order their IR transforms run. The order is explicit because each
   * transform sees the IR left behind by the ones before it.
   */
  val ALL: List<LithoCompilerPlugin> = listOf(HookIdentityPlugin)
}
