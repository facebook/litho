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

import com.facebook.litho.Component.RenderData
import com.facebook.litho.internal.HookKey

/**
 * Keeps track of the last mounted @Prop/@State a component was rendered with for components that
 * care about them (currently, this is just for ComponentSpecs that use [Diff]'s of props in any of
 * their lifecycle methods).
 */
class RenderState(from: RenderState? = null) {

  private var layoutStateId: Int = from?.layoutStateId ?: LayoutState.NO_PREVIOUS_LAYOUT_STATE_ID
  private val renderData: MutableMap<HookKey, RenderData> = HashMap(from?.renderData.orEmpty())

  fun recordRenderData(layoutState: LayoutState) {
    // Record the layout state id for the next layout calculation.
    layoutStateId = layoutState.id

    // Record render data
    val seenHookKeys = HashSet<HookKey>()
    for (creator in layoutState.transitionData?.transitionCreators.orEmpty()) {
      // Sanity check like in StateHandler
      val hookKey = creator.identityKey
      if (!seenHookKeys.add(hookKey)) {
        // We found two components with the same global key.
        throw RuntimeException(
            "Cannot record render data for KComponent, found another Component with the same key: ${hookKey.globalKey}")
      }
      renderData[hookKey] = creator.recordRenderData()
    }
  }

  internal fun getPreviousRenderData(hookKey: HookKey): RenderData? = renderData[hookKey]

  fun getPreviousLayoutStateId(): Int = layoutStateId
}
