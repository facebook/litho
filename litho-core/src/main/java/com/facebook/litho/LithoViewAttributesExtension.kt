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
import android.view.View
import androidx.collection.MutableScatterMap
import androidx.core.view.ViewCompat
import com.facebook.litho.LithoViewAttributesExtension.LithoViewAttributesState
import com.facebook.litho.LithoViewAttributesExtension.ViewAttributesInput
import com.facebook.litho.ViewAttributes.Companion.setViewAttributes
import com.facebook.litho.ViewAttributes.Companion.unsetViewAttributes
import com.facebook.rendercore.MountState
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.OnItemCallbacks
import com.facebook.rendercore.utils.equals

class LithoViewAttributesExtension
private constructor(
    private val useFineGrainedAttributesState: Boolean,
    private val cloneStateListAnimators: Boolean
) :
    MountExtension<ViewAttributesInput, LithoViewAttributesState>(),
    OnItemCallbacks<LithoViewAttributesState> {

  interface ViewAttributesInput {
    val viewAttributes: Map<Long, ViewAttributes>?
  }

  override fun createState(): LithoViewAttributesState =
      if (useFineGrainedAttributesState)
          FineGrainedLithoViewAttributesState(cloneStateListAnimators)
      else DefaultLithoViewAttributesState(cloneStateListAnimators)

  interface LithoViewAttributesState {
    val shouldCloneStateListAnimators: Boolean

    fun onUnitMounted(id: Long)

    fun onUnitUnmounted(id: Long)

    fun hasDefaultViewAttributes(renderUnitId: Long): Boolean

    fun setDefaultViewAttributes(renderUnitId: Long, flags: Int)

    fun getDefaultViewAttributes(renderUnitId: Long): Int

    fun getCurrentViewAttributes(id: Long): ViewAttributes?

    fun getNewViewAttributes(id: Long): ViewAttributes?

    /** This method should be called ahead of a new iteration of the state being called. */
    fun prepare(newUnitsAttributes: Map<Long, ViewAttributes>?)

    fun commit()

    fun reset()
  }

  private class DefaultLithoViewAttributesState(
      override val shouldCloneStateListAnimators: Boolean
  ) : LithoViewAttributesState {
    private val _defaultViewAttributes: MutableMap<Long, Int> = HashMap()
    private var currentUnits: Map<Long, ViewAttributes>? = null
    private var newUnits: Map<Long, ViewAttributes>? = null

    override fun prepare(newUnitsAttributes: Map<Long, ViewAttributes>?) {
      newUnits = newUnitsAttributes
    }

    override fun commit() {
      currentUnits = newUnits
    }

    override fun reset() {
      currentUnits = null
      newUnits = null
    }

    override fun setDefaultViewAttributes(renderUnitId: Long, flags: Int) {
      _defaultViewAttributes[renderUnitId] = flags
    }

    override fun getDefaultViewAttributes(renderUnitId: Long): Int {
      return _defaultViewAttributes[renderUnitId]
          ?: throw IllegalStateException(
              "View attributes not found, did you call onUnbindItem without onBindItem?")
    }

    override fun hasDefaultViewAttributes(renderUnitId: Long): Boolean =
        _defaultViewAttributes.containsKey(renderUnitId)

    override fun getCurrentViewAttributes(id: Long): ViewAttributes? = currentUnits?.get(id)

    override fun getNewViewAttributes(id: Long): ViewAttributes? = newUnits?.get(id)

    override fun onUnitMounted(id: Long) = Unit

    override fun onUnitUnmounted(id: Long) = Unit
  }

  private class FineGrainedLithoViewAttributesState(
      override val shouldCloneStateListAnimators: Boolean
  ) : LithoViewAttributesState {
    private val _defaultViewAttributes: MutableMap<Long, Int> = HashMap()
    private val currentUnits: MutableScatterMap<Long, ViewAttributes> = MutableScatterMap()
    private var toBeCommittedUnits: Map<Long, ViewAttributes>? = null

    override fun prepare(newUnitsAttributes: Map<Long, ViewAttributes>?) {
      toBeCommittedUnits = newUnitsAttributes
    }

    override fun commit() {
      toBeCommittedUnits?.let { currentUnits.putAll(it) }
    }

    override fun reset() {
      currentUnits.clear()
      toBeCommittedUnits = null
    }

    override fun setDefaultViewAttributes(renderUnitId: Long, flags: Int) {
      _defaultViewAttributes[renderUnitId] = flags
    }

    override fun getDefaultViewAttributes(renderUnitId: Long): Int {
      return _defaultViewAttributes[renderUnitId]
          ?: throw IllegalStateException(
              "View attributes not found, did you call onUnbindItem without onBindItem? | $renderUnitId")
    }

    override fun hasDefaultViewAttributes(renderUnitId: Long): Boolean =
        _defaultViewAttributes.containsKey(renderUnitId)

    override fun getCurrentViewAttributes(id: Long): ViewAttributes? = currentUnits[id]

    override fun getNewViewAttributes(id: Long): ViewAttributes? = toBeCommittedUnits?.get(id)

    override fun onUnitMounted(id: Long) {
      toBeCommittedUnits?.get(id)?.let { currentUnits[id] = it }
    }

    override fun onUnitUnmounted(id: Long) {
      currentUnits.remove(id)
    }
  }

  override fun beforeMount(
      extensionState: ExtensionState<LithoViewAttributesState>,
      input: ViewAttributesInput?,
      localVisibleRect: Rect?
  ) {
    if (input != null) {
      extensionState.state.prepare(input.viewAttributes)
    }
  }

  override fun afterMount(extensionState: ExtensionState<LithoViewAttributesState>) {
    extensionState.state.commit()
  }

  override fun onMountItem(
      extensionState: ExtensionState<LithoViewAttributesState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ) {
    val state = extensionState.state
    val id = renderUnit.id
    val viewAttributes = state.getNewViewAttributes(id)

    if (viewAttributes != null) {
      // Get the initial view attribute flags for the root LithoView.
      if (!state.hasDefaultViewAttributes(id)) {
        val flags =
            if (renderUnit.id == MountState.ROOT_HOST_ID) {
              (content as BaseMountingView).mViewAttributeFlags
            } else {
              LithoMountData.getViewAttributeFlags(content)
            }
        state.setDefaultViewAttributes(id, flags)
      }
      setViewAttributes(content, viewAttributes, renderUnit, state.shouldCloneStateListAnimators)
      state.onUnitMounted(id)
    }
  }

  override fun onUnmountItem(
      extensionState: ExtensionState<LithoViewAttributesState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ) {
    val state = extensionState.state
    val id = renderUnit.id
    val viewAttributes = state.getCurrentViewAttributes(id)
    if (viewAttributes != null) {
      val flags = state.getDefaultViewAttributes(id)
      unsetViewAttributes(content, viewAttributes, flags)
      state.onUnitUnmounted(id)
    }
  }

  override fun beforeMountItem(
      extensionState: ExtensionState<LithoViewAttributesState>,
      renderTreeNode: RenderTreeNode,
      index: Int
  ) = Unit

  override fun onBindItem(
      extensionState: ExtensionState<LithoViewAttributesState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ) = Unit

  override fun onUnbindItem(
      extensionState: ExtensionState<LithoViewAttributesState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?
  ) = Unit

  override fun onBoundsAppliedToItem(
      extensionState: ExtensionState<LithoViewAttributesState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?,
      changed: Boolean
  ) {
    if (content is View) {
      val state = extensionState.state
      val id = renderUnit.id
      val attrs = state.getNewViewAttributes(id)

      if (attrs != null && !attrs.systemGestureExclusionZones.isNullOrEmpty()) {
        val bounds = Rect(content.left, content.top, content.right, content.bottom)
        val exclusions = attrs.systemGestureExclusionZones?.let { it.map { e -> e(bounds) } }
        if (exclusions != null) {
          ViewCompat.setSystemGestureExclusionRects(content, exclusions)
        }
      }
    }
  }

  override fun shouldUpdateItem(
      extensionState: ExtensionState<LithoViewAttributesState>,
      previousRenderUnit: RenderUnit<*>,
      previousLayoutData: Any?,
      nextRenderUnit: RenderUnit<*>,
      nextLayoutData: Any?
  ): Boolean {
    if (previousRenderUnit === nextRenderUnit) {
      return false
    }
    val id = previousRenderUnit.id
    val state = extensionState.state
    val currentAttributes = state.getCurrentViewAttributes(id)
    val nextAttributes = state.getNewViewAttributes(id)
    return (previousRenderUnit is MountSpecLithoRenderUnit &&
        nextRenderUnit is MountSpecLithoRenderUnit &&
        MountSpecLithoRenderUnit.shouldUpdateMountItem(
            previousRenderUnit, nextRenderUnit, previousLayoutData, nextLayoutData)) ||
        shouldUpdateViewInfo(nextAttributes, currentAttributes)
  }

  override fun onUnmount(extensionState: ExtensionState<LithoViewAttributesState>) {
    extensionState.state.reset()
  }

  companion object {
    @JvmStatic
    fun getInstance(
        useFineGrainedAttributesState: Boolean,
        cloneStateListAnimators: Boolean = false,
    ): LithoViewAttributesExtension =
        LithoViewAttributesExtension(useFineGrainedAttributesState, cloneStateListAnimators)

    @JvmStatic
    fun shouldUpdateViewInfo(
        nextAttributes: ViewAttributes?,
        currentAttributes: ViewAttributes?
    ): Boolean = !equals(currentAttributes, nextAttributes)
  }
}
