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

import android.graphics.Rect
import androidx.annotation.VisibleForTesting
import com.facebook.litho.DynamicPropsExtension.DynamicPropsExtensionState
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.OnItemCallbacks

object DynamicPropsExtension :
    MountExtension<DynamicPropsExtensionInput, DynamicPropsExtensionState>(),
    OnItemCallbacks<DynamicPropsExtensionState> {

  override fun createState(): DynamicPropsExtensionState {
    return DynamicPropsExtensionState()
  }

  override fun beforeMount(
      extensionState: ExtensionState<DynamicPropsExtensionState>,
      input: DynamicPropsExtensionInput?,
      localVisibleRect: Rect?
  ) {
    val state: DynamicPropsExtensionState = extensionState.state
    state.previousInput = state.currentInput
    state.currentInput = input?.dynamicValueOutputs
  }

  override fun onUnmount(extensionState: ExtensionState<DynamicPropsExtensionState>) {
    extensionState.releaseAllAcquiredReferences()
    val state: DynamicPropsExtensionState = extensionState.state
    state.currentInput = null
    state.previousInput = null
  }

  override fun afterMount(extensionState: ExtensionState<DynamicPropsExtensionState>) {
    val state: DynamicPropsExtensionState = extensionState.state
    state.previousInput = null
  }

  override fun onBindItem(
      extensionState: ExtensionState<DynamicPropsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ) {
    val state: DynamicPropsExtensionState = extensionState.state
    val dynamicValueOutput: DynamicValueOutput? = state.currentInput?.get(renderUnit.id)
    if (dynamicValueOutput != null) {
      state.dynamicPropsManager.onBindComponentToContent(
          dynamicValueOutput.component,
          dynamicValueOutput.scopedContext,
          dynamicValueOutput.commonDynamicProps,
          content)
    }
  }

  override fun onUnbindItem(
      extensionState: ExtensionState<DynamicPropsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ) {
    val state: DynamicPropsExtensionState = extensionState.state
    val dynamicValueOutput: DynamicValueOutput? =
        state.previousInput?.get(renderUnit.id) ?: state.currentInput?.get(renderUnit.id)
    if (dynamicValueOutput != null) {
      state.dynamicPropsManager.onUnbindComponent(
          dynamicValueOutput.component, dynamicValueOutput.commonDynamicProps, content)
    }
  }

  override fun shouldUpdateItem(
      extensionState: ExtensionState<DynamicPropsExtensionState>,
      previousRenderUnit: RenderUnit<*>,
      previousLayoutData: Any?,
      nextRenderUnit: RenderUnit<*>,
      nextLayoutData: Any?
  ): Boolean = true

  override fun beforeMountItem(
      extensionState: ExtensionState<DynamicPropsExtensionState>,
      renderTreeNode: RenderTreeNode,
      index: Int
  ): Unit = Unit

  override fun onMountItem(
      extensionState: ExtensionState<DynamicPropsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ): Unit = Unit

  override fun onUnmountItem(
      extensionState: ExtensionState<DynamicPropsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ): Unit = Unit

  override fun onBoundsAppliedToItem(
      extensionState: ExtensionState<DynamicPropsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?,
      changed: Boolean
  ): Unit = Unit

  class DynamicPropsExtensionState {
    @get:VisibleForTesting val dynamicPropsManager: DynamicPropsManager = DynamicPropsManager()
    internal var currentInput: Map<Long, DynamicValueOutput>? = null
    internal var previousInput: Map<Long, DynamicValueOutput>? = null
  }
}
