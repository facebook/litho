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
import com.facebook.litho.transition.TransitionWithDependency

/**
 * Keeps track of the last mounted @Prop/@State a component was rendered with for components that
 * care about them (currently, this is just for ComponentSpecs that use [Diff]'s of props in any of
 * their lifecycle methods).
 */
class RenderState(from: RenderState? = null) {

  private var layoutStateId: Int = from?.layoutStateId ?: LayoutState.NO_PREVIOUS_LAYOUT_STATE_ID
  private val renderData: MutableMap<HookKey, RenderData?> = HashMap(from?.renderData.orEmpty())
  private val seenHookKeys: MutableSet<HookKey> = HashSet(from?.seenHookKeys.orEmpty())

  fun recordRenderData(layoutState: LayoutState) {
    // Record the layout state id for the next layout calculation.
    layoutStateId = layoutState.id

    // Record render data for Spec-Gen components
    val scopedComponentInfos = layoutState.scopedComponentInfosNeedingPreviousRenderData
    if (!scopedComponentInfos.isNullOrEmpty()) recordRenderData(scopedComponentInfos)

    // Record render data for KComponents
    val definitions = layoutState.transitionData?.transitionsWithDependency
    if (!definitions.isNullOrEmpty()) recordKRenderData(definitions)

    // Reset seen global keys for the next record phase
    seenHookKeys.clear()
  }

  internal fun getPreviousRenderData(hookKey: HookKey): RenderData? = renderData[hookKey]

  fun applyPreviousRenderData(scopedComponentInfos: List<ScopedComponentInfo>?) {
    if (scopedComponentInfos == null) {
      return
    }
    for (componentInfo in scopedComponentInfos) {
      applyPreviousRenderData(componentInfo)
    }
  }

  fun getPreviousLayoutStateId(): Int = layoutStateId

  private fun recordRenderData(scopedComponentInfos: List<ScopedComponentInfo>) {
    for (componentInfo in scopedComponentInfos) {
      recordRenderData(componentInfo)
    }
  }

  private fun recordKRenderData(definitions: List<TransitionWithDependency>) {
    for (def in definitions) {
      // Sanity check like in StateHandler
      val hookKey = def.identityKey
      if (!seenHookKeys.add(hookKey)) {
        // We found two components with the same global key.
        throw RuntimeException(
            "Cannot record render data for KComponent, found another Component with the same key: ${hookKey.globalKey}")
      }
      renderData[hookKey] = def.recordRenderData()
    }
  }

  private fun recordRenderData(scopedComponentInfo: ScopedComponentInfo) {
    val component = scopedComponentInfo.component
    val hookKey = HookKey(scopedComponentInfo.context.globalKey, 0)
    if (!isPreviousRenderDataSupported(component)) {
      throw RuntimeException(
          "Trying to record previous render data for component that doesn't support it")
    }

    // Sanity check like in StateHandler
    if (!seenHookKeys.add(hookKey)) {
      // We found two components with the same global key.
      throw RuntimeException(
          "Cannot record previous render data for ${component.simpleName}, found another Component with the same key: $hookKey")
    }
    val currentInfo = renderData[hookKey]
    renderData[hookKey] =
        (component as SpecGeneratedComponent).recordRenderData(
            scopedComponentInfo.context, currentInfo)
  }

  private fun applyPreviousRenderData(scopedComponentInfo: ScopedComponentInfo) {
    val component = scopedComponentInfo.component
    val hookKey = HookKey(scopedComponentInfo.context.globalKey, 0)
    if (!isPreviousRenderDataSupported(component)) {
      throw RuntimeException(
          "Trying to apply previous render data to component that doesn't support it")
    }

    (component as SpecGeneratedComponent).applyPreviousRenderData(renderData[hookKey])
  }

  private fun isPreviousRenderDataSupported(component: Component): Boolean =
      component is SpecGeneratedComponent && component.needsPreviousRenderData()
}
