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

package com.facebook.rendercore.incrementalmount

import android.util.Pair
import com.facebook.rendercore.MountState
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.testing.TestRenderCoreExtension

/**
 * Registers [extension] through the supported [MountState.addExtensions] path (the imperative
 * registerMountExtension is deprecated) and returns the created [ExtensionState].
 */
fun <State> MountState.registerExtension(
    extension: MountExtension<*, State>,
): ExtensionState<State> {
  addExtensions(
      listOf(Pair<RenderCoreExtension<*, *>, Any>(TestRenderCoreExtension(extension), Unit)),
  )
  @Suppress("UNCHECKED_CAST")
  return checkNotNull(
      getMountDelegate()?.extensionStates?.firstOrNull { it.extension === extension },
  )
      as ExtensionState<State>
}
