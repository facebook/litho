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

import android.annotation.SuppressLint

object LithoKeyTestingUtil {

  /**
   * Map each component type to a list of the global keys of the components of that type in the
   * order in which they are added to the hierarchy.
   */
  @JvmStatic
  @SuppressLint("RestrictedApi")
  fun getScopedComponentInfos(lithoView: LithoView): Map<String, List<ScopedComponentInfo>> {
    val scopedComponentInfos: MutableMap<String, MutableList<ScopedComponentInfo>> = LinkedHashMap()
    lithoView.componentTree?.committedLayoutState?.rootLayoutResult?.node?.let { node ->
      addScopedComponentInfoForNode(node, scopedComponentInfos)
    }
    return scopedComponentInfos
  }

  @JvmStatic
  fun addScopedComponentInfoForNode(
      node: LithoNode,
      scopedComponentInfos: MutableMap<String, MutableList<ScopedComponentInfo>>
  ) {
    for (i in 0 until node.componentCount) {
      val component: Component = node.getComponentAt(i)
      val componentType: String = component.simpleName

      val sameTypeKeys = scopedComponentInfos.getOrPut(componentType) { mutableListOf() }
      sameTypeKeys.add(node.getComponentInfoAt(i))
    }

    for (i in 0 until node.childCount) {
      addScopedComponentInfoForNode(node.getChildAt(i), scopedComponentInfos)
    }
  }
}
