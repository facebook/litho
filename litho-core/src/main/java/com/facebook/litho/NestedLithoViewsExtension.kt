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

import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.OnItemCallbacks

/**
 * MountExtension to ensure that content with nested LithoViews is properly clearing those
 * LithoViews when the item is unmounted. Since this should only happen when unmounting an item and
 * not when it's being updated, shouldUpdateItem is not overridden (defaulting to super
 * implementation which returns false).
 */
class NestedLithoViewsExtension : MountExtension<Void?, Void?>(), OnItemCallbacks<Void?> {
  override fun createState(): Void? = null

  override fun onUnmountItem(
      extensionState: ExtensionState<Void?>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ) {
    if (content is HasLithoViewChildren) {
      val baseMountingViews = ArrayList<BaseMountingView>()
      content.obtainLithoViewChildren(baseMountingViews)
      for (i in baseMountingViews.indices.reversed()) {
        val baseMountingView = baseMountingViews[i]
        baseMountingView.unmountAllItems()
      }
    }
  }

  override fun beforeMountItem(
      extensionState: ExtensionState<Void?>,
      renderTreeNode: RenderTreeNode,
      index: Int
  ): Unit = Unit

  override fun onMountItem(
      extensionState: ExtensionState<Void?>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ): Unit = Unit

  override fun shouldUpdateItem(
      extensionState: ExtensionState<Void?>,
      previousRenderUnit: RenderUnit<*>,
      previousLayoutData: Any?,
      nextRenderUnit: RenderUnit<*>,
      nextLayoutData: Any?
  ): Boolean = false

  override fun onBindItem(
      extensionState: ExtensionState<Void?>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ): Unit = Unit

  override fun onUnbindItem(
      extensionState: ExtensionState<Void?>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ): Unit = Unit

  override fun onBoundsAppliedToItem(
      extensionState: ExtensionState<Void?>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?,
      changed: Boolean
  ): Unit = Unit
}
