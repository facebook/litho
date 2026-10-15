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

import androidx.collection.ScatterSet
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.state.StateId
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.visibility.VisibilityOutput

/**
 * A lightweight representation of a layout node, used to cache measurements between two Layout tree
 * calculations.
 */
interface DiffNode : Cloneable {

  val childCount: Int

  fun getChildAt(i: Int): DiffNode?

  val component: Component
  val componentGlobalKey: GlobalKey
  val scopedComponentInfo: ScopedComponentInfo
  var layoutData: Any?
  var effects: List<Attachable>?
  var stateReads: ScatterSet<StateId>?

  /**
   * The last value the measure function associated with this node [Component] returned for the
   * width. This is used together with [lastWidthSpec] to implement measure caching. Also sets the
   * last value the measure function associated with this node [Component] returned for the width.
   */
  var lastMeasuredWidth: Int
  /**
   * The last value the measure function associated with this node [Component] returned for the
   * height. This is used together with [lastHeightSpec] to implement measure caching. Also sets the
   * last value the measure function associated with this node [Component] returned for the height.
   */
  var lastMeasuredHeight: Int
  var lastWidthSpec: Int
  var lastHeightSpec: Int
  /**
   * The direction this node was last laid out in. A layout can depend on it, so its last
   * measurement is only reused in the same direction.
   */
  var layoutDirection: LayoutDirection?
  val children: List<DiffNode>

  fun addChild(node: DiffNode)

  var contentOutput: LithoRenderUnit?
  var visibilityOutput: VisibilityOutput?
  var backgroundOutput: LithoRenderUnit?
  var foregroundOutput: LithoRenderUnit?
  var borderOutput: LithoRenderUnit?
  var hostOutput: LithoRenderUnit?
  var delegate: LayoutResult?
  var primitive: Primitive?

  companion object {
    const val UNSPECIFIED: Int = -1
  }
}
