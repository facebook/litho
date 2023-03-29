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
import java.lang.RuntimeException
import java.util.HashMap
import java.util.HashSet

/**
 * Keeps track of the last mounted @Prop/@State a component was rendered with for components that
 * care about them (currently, this is just for ComponentSpecs that use [Diff]'s of props in any of
 * their lifecycle methods).
 */
class RenderState {

  private val renderData: MutableMap<String, RenderData?> = HashMap()
  private val seenGlobalKeys: MutableSet<String> = HashSet()

  fun recordRenderData(scopedComponentInfos: List<ScopedComponentInfo>?) {
    if (scopedComponentInfos == null) {
      return
    }

    for (componentInfo in scopedComponentInfos) {
      recordRenderData(componentInfo)
    }
    seenGlobalKeys.clear()
  }

  fun applyPreviousRenderData(scopedComponentInfos: List<ScopedComponentInfo>?) {
    if (scopedComponentInfos == null) {
      return
    }
    for (componentInfo in scopedComponentInfos) {
      applyPreviousRenderData(componentInfo)
    }
  }

  private fun recordRenderData(scopedComponentInfo: ScopedComponentInfo) {
    val component = scopedComponentInfo.component
    val globalKey = scopedComponentInfo.context.globalKey
    if (!isPreviousRenderDataSupported(component)) {
      throw RuntimeException(
          "Trying to record previous render data for component that doesn't support it")
    }

    // Sanity check like in StateHandler
    if (seenGlobalKeys.contains(globalKey)) {
      // We found two components with the same global key.
      throw RuntimeException(
          "Cannot record previous render data for ${component.simpleName}, found another Component with the same key: $globalKey")
    }
    seenGlobalKeys.add(globalKey)
    val currentInfo = renderData[globalKey]
    renderData[globalKey] =
        (component as SpecGeneratedComponent).recordRenderData(
            scopedComponentInfo.context, currentInfo)
  }

  private fun applyPreviousRenderData(scopedComponentInfo: ScopedComponentInfo) {
    val component = scopedComponentInfo.component
    val globalKey = scopedComponentInfo.context.globalKey
    if (!isPreviousRenderDataSupported(component)) {
      throw RuntimeException(
          "Trying to apply previous render data to component that doesn't support it")
    }

    (component as SpecGeneratedComponent).applyPreviousRenderData(renderData[globalKey])
  }

  private fun isPreviousRenderDataSupported(component: Component): Boolean =
      component is SpecGeneratedComponent && component.needsPreviousRenderData()
}
