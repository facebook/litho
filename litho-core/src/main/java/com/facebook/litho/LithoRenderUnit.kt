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

import android.util.SparseArray
import com.facebook.litho.annotations.ImportantForAccessibility
import com.facebook.rendercore.MountItem
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.transitions.TransitionRenderUnit

abstract class LithoRenderUnit
protected constructor(
    private val _id: Long,
    val component: Component,
    @field:JvmField val commonDynamicProps: SparseArray<DynamicValue<Any?>>?,
    val nodeInfo: NodeInfo?,
    val flags: Int,
    importantForAccessibility: Int,
    renderType: RenderType?,
    @field:JvmField val componentContext: ComponentContext?,
    private val debugKey: String?
) : RenderUnit<Any?>(renderType), TransitionRenderUnit {

  override fun getId(): Long = _id

  override fun getDebugKey(): String = debugKey ?: componentContext?.globalKey ?: _id.toString()

  // TODO: remove
  var hierarchy: DebugHierarchy.Node? = null

  // the A11Y prop for descendants has been corrected
  val importantForAccessibility: Int =
      if (importantForAccessibility ==
          ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS) {
        ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES
      } else {
        importantForAccessibility
      }

  override fun getMatchHostBounds(): Boolean = flags and LAYOUT_FLAG_MATCH_HOST_BOUNDS != 0

  val contentDescription: CharSequence?
    get() = nodeInfo?.contentDescription

  val isAccessible: Boolean
    get() {
      if (importantForAccessibility == ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO) {
        return false
      }

      return (this.nodeInfo != null && nodeInfo.needsAccessibilityDelegate()) ||
          (this.component is SpecGeneratedComponent && this.component.implementsAccessibility())
    }

  companion object {
    const val LAYOUT_FLAG_DUPLICATE_PARENT_STATE = 1 shl 0
    const val LAYOUT_FLAG_DISABLE_TOUCHABLE = 1 shl 1
    const val LAYOUT_FLAG_MATCH_HOST_BOUNDS = 1 shl 2
    const val LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED = 1 shl 3
    const val LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES = 1 shl 4
    const val LAYOUT_FLAG_HAS_TOUCH_EVENT_HANDLERS = 1 shl 5

    @JvmStatic
    fun getComponentContext(item: MountItem): ComponentContext? =
        (item.renderTreeNode.renderUnit as LithoRenderUnit).componentContext

    @JvmStatic
    fun getComponentContext(node: RenderTreeNode): ComponentContext? =
        (node.renderUnit as LithoRenderUnit).componentContext

    @JvmStatic
    fun getComponentContext(unit: LithoRenderUnit): ComponentContext? = unit.componentContext

    @JvmStatic
    fun getRenderUnit(item: MountItem): LithoRenderUnit = getRenderUnit(item.renderTreeNode)

    @JvmStatic
    fun getRenderUnit(node: RenderTreeNode): LithoRenderUnit = node.renderUnit as LithoRenderUnit

    @JvmStatic
    fun isDuplicateParentState(flags: Int): Boolean =
        flags and LAYOUT_FLAG_DUPLICATE_PARENT_STATE == LAYOUT_FLAG_DUPLICATE_PARENT_STATE

    @JvmStatic
    fun hasTouchEventHandlers(flags: Int): Boolean =
        flags and LAYOUT_FLAG_HAS_TOUCH_EVENT_HANDLERS == LAYOUT_FLAG_HAS_TOUCH_EVENT_HANDLERS

    @JvmStatic
    fun isDuplicateChildrenStates(flags: Int): Boolean =
        flags and LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES == LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES

    @JvmStatic
    fun isTouchableDisabled(flags: Int): Boolean =
        flags and LAYOUT_FLAG_DISABLE_TOUCHABLE == LAYOUT_FLAG_DISABLE_TOUCHABLE

    @JvmStatic
    fun areDrawableOutputsDisabled(flags: Int): Boolean =
        flags and LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED != 0

    @JvmStatic
    fun isMountableView(unit: RenderUnit<*>): Boolean = unit.renderType == RenderType.VIEW
  }
}
