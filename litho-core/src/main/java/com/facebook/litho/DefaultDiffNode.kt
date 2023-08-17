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

import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.Mountable
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.visibility.VisibilityOutput
import java.util.ArrayList

class DefaultDiffNode
internal constructor(
    override val component: Component,
    override val componentGlobalKey: String,
    override val scopedComponentInfo: ScopedComponentInfo
) : DiffNode {

  private val _children: MutableList<DiffNode> = ArrayList(4)
  override var contentOutput: LithoRenderUnit? = null
  override var backgroundOutput: LithoRenderUnit? = null
  override var foregroundOutput: LithoRenderUnit? = null
  override var borderOutput: LithoRenderUnit? = null
  override var hostOutput: LithoRenderUnit? = null
  override var visibilityOutput: VisibilityOutput? = null
  override var mountable: Mountable<*>? = null
  override var primitive: Primitive? = null
  override var lastMeasuredWidth: Float = 0f
  override var lastMeasuredHeight: Float = 0f
  override var lastWidthSpec: Int = 0
  override var lastHeightSpec: Int = 0
  override var delegate: LayoutResult? = null
  override var layoutData: Any? = null

  override val childCount: Int
    get() = _children.size

  override fun getChildAt(i: Int): DiffNode = _children[i]

  override val children: List<DiffNode>
    get() = _children

  override fun addChild(node: DiffNode) {
    _children.add(node)
  }
}
