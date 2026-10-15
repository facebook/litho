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

import android.graphics.Color
import android.util.SparseArray
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import com.facebook.litho.CollectionsUtils.mergeSparseArrays
import com.facebook.litho.view.ClickListenerHolder
import com.facebook.litho.visibility.Visibility
import com.facebook.rendercore.utils.equals
import com.facebook.rendercore.utils.isEquivalentTo

/**
 * Contains the 23 view attribute properties from [NodeInfo] that have been migrated to the
 * `onBindViewWithDescription` binder pattern behind the `useViewStyleBinders` config flag.
 *
 * When the binder migration is fully rolled out and `useViewStyleBinders` is removed, this entire
 * class can be deleted.
 */
class BinderMigratedNodeInfo {

  var tooltipText: String? = null

  var viewId: Int = View.NO_ID
    set(value) {
      field = value
      flags = flags or FLAG_VIEW_ID
    }

  var viewTag: Any? = null
    set(value) {
      field = value
      flags = flags or FLAG_VIEW_TAG
    }

  var transitionName: String? = null

  var outlineProvider: ViewOutlineProvider? = null
  var renderEffect: LithoRenderEffect? = null

  var clickHandler: EventHandler<ClickEvent>? = null
    set(value) {
      field = value
      // Setting a NEW (non-null) primary clears any extra click listeners that were layered on
      // top of the previous primary — matches the "last `onClick` wins" semantics. Enforced
      // here, at render time, so the dispatch order at mount is fully determined by Style chain
      // evaluation (no mount-time mutation).
      //
      // A null assignment (e.g., disabled `Style.onClick(enabled = false, …)`) does NOT clear
      // extras — otherwise a chain like `clickListener(KEY) { … }.onClick(enabled = isOn) { … }`
      // would silently drop the telemetry listener when `isOn` is false.
      if (value != null) {
        extraClickListeners = null
      }
    }

  var longClickHandler: EventHandler<LongClickEvent>? = null
  var focusChangeHandler: EventHandler<FocusChangedEvent>? = null
  var touchHandler: EventHandler<TouchEvent>? = null
  var interceptTouchHandler: EventHandler<InterceptTouchEvent>? = null

  /**
   * Additional click listeners registered alongside [clickHandler]. Insertion order is preserved.
   * Dispatch order at click: primary first, then each additional in this list's order.
   *
   * Lazily allocated. `null` when no additionals have been added.
   */
  internal var extraClickListeners: MutableList<ClickListenerHolder>? = null

  internal fun addExtraClickListener(holder: ClickListenerHolder) {
    val list =
        extraClickListeners ?: ArrayList<ClickListenerHolder>(2).also { extraClickListeners = it }
    for (i in list.indices) {
      check(list[i].key != holder.key) {
        "Click listener for key 0x${Integer.toHexString(holder.key)} is already registered on this node."
      }
    }
    list.add(holder)
  }

  internal var flags: Int = 0
    private set

  var scale: Float = 1f
    set(value) {
      field = value
      flags =
          if (value == 1f) {
            flags and FLAG_SCALE.inv()
          } else {
            flags or FLAG_SCALE
          }
    }

  var alpha: Float = 1f
    set(value) {
      field = value
      flags =
          if (value == 1f) {
            flags and FLAG_ALPHA.inv()
          } else {
            flags or FLAG_ALPHA
          }
    }

  var rotation: Float = 0f
    set(value) {
      field = value
      flags =
          if (value == 0f) {
            flags and FLAG_ROTATION.inv()
          } else {
            flags or FLAG_ROTATION
          }
    }

  var rotationX: Float = 0f
    set(value) {
      field = value
      flags = flags or FLAG_ROTATION_X
    }

  var rotationY: Float = 0f
    set(value) {
      field = value
      flags = flags or FLAG_ROTATION_Y
    }

  var clipChildren: Boolean = true // Default value for ViewGroup
    set(value) {
      field = value
      flags = flags or FLAG_CLIP_CHILDREN
    }

  var defaultFocusHighlightEnabled: Boolean = true // Platform default is true
    set(value) {
      field = value
      flags = flags or FLAG_DEFAULT_FOCUS_HIGHLIGHT_ENABLED
    }

  var clipToOutline: Boolean = false
    set(value) {
      field = value
      flags = flags or FLAG_CLIP_TO_OUTLINE
    }

  var shadowElevation: Float = 0f
    set(value) {
      field = value
      flags = flags or FLAG_SHADOW_ELEVATION
    }

  @ColorInt
  var ambientShadowColor: Int = Color.BLACK
    set(value) {
      field = value
      flags = flags or FLAG_AMBIENT_SHADOW_COLOR
    }

  @ColorInt
  var spotShadowColor: Int = Color.BLACK
    set(value) {
      field = value
      flags = flags or FLAG_SPOT_SHADOW_COLOR
    }

  @NodeInfo.FocusState var focusState: Int = NodeInfo.FOCUS_UNSET

  @NodeInfo.ClickableState var clickableState: Int = NodeInfo.CLICKABLE_UNSET

  @NodeInfo.EnabledState var enabledState: Int = NodeInfo.ENABLED_UNSET

  @NodeInfo.SelectedState var selectedState: Int = NodeInfo.SELECTED_UNSET

  @NodeInfo.KeyboardNavigationClusterState
  var keyboardNavigationClusterState: Int = NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_UNSET

  var visibility: Visibility? = null

  fun setFocusable(isFocusable: Boolean) {
    focusState = if (isFocusable) NodeInfo.FOCUS_SET_TRUE else NodeInfo.FOCUS_SET_FALSE
  }

  fun setClickable(isClickable: Boolean) {
    clickableState = if (isClickable) NodeInfo.CLICKABLE_SET_TRUE else NodeInfo.CLICKABLE_SET_FALSE
  }

  fun setEnabled(isEnabled: Boolean) {
    enabledState = if (isEnabled) NodeInfo.ENABLED_SET_TRUE else NodeInfo.ENABLED_SET_FALSE
  }

  fun setSelected(isSelected: Boolean) {
    selectedState = if (isSelected) NodeInfo.SELECTED_SET_TRUE else NodeInfo.SELECTED_SET_FALSE
  }

  fun setKeyboardNavigationCluster(isKeyboardNavigationCluster: Boolean) {
    keyboardNavigationClusterState =
        if (isKeyboardNavigationCluster) {
          NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_TRUE
        } else {
          NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_FALSE
        }
  }

  val isClipChildrenSet: Boolean
    get() = flags and FLAG_CLIP_CHILDREN != 0

  val isDefaultFocusHighlightEnabledSet: Boolean
    get() = flags and FLAG_DEFAULT_FOCUS_HIGHLIGHT_ENABLED != 0

  val isScaleSet: Boolean
    get() = flags and FLAG_SCALE != 0

  val isAlphaSet: Boolean
    get() = flags and FLAG_ALPHA != 0

  val isRotationSet: Boolean
    get() = flags and FLAG_ROTATION != 0

  val isRotationXSet: Boolean
    get() = flags and FLAG_ROTATION_X != 0

  val isRotationYSet: Boolean
    get() = flags and FLAG_ROTATION_Y != 0

  val isTagSet: Boolean
    get() = flags and FLAG_VIEW_TAG != 0

  val isViewIdSet: Boolean
    get() = flags and FLAG_VIEW_ID != 0

  var viewTags: SparseArray<Any>? = null

  fun addViewTag(id: Int, tag: Any) {
    if (viewTags == null) {
      viewTags = SparseArray<Any>()
    }
    viewTags?.put(id, tag)
  }

  fun addViewTags(viewTags: SparseArray<Any>?) {
    if (this.viewTags == null) {
      this.viewTags = viewTags
    } else {
      this.viewTags = mergeSparseArrays(this.viewTags, viewTags)
    }
  }

  fun hasViewId(): Boolean = flags and FLAG_VIEW_ID != 0

  fun hasFocusChangeHandler(): Boolean = focusChangeHandler != null

  fun hasTouchEventHandlers(): Boolean =
      clickHandler != null ||
          longClickHandler != null ||
          touchHandler != null ||
          interceptTouchHandler != null

  fun copyInto(target: BinderMigratedNodeInfo) {
    tooltipText?.let { target.tooltipText = it }
    if (hasViewId()) {
      target.viewId = viewId
    }
    viewTag?.let { target.viewTag = it }
    transitionName?.let { target.transitionName = it }
    viewTags?.let { target.addViewTags(it) }
    outlineProvider?.let { target.outlineProvider = it }
    renderEffect?.let { target.renderEffect = it }

    clickHandler?.let { target.clickHandler = it }
    longClickHandler?.let { target.longClickHandler = it }
    focusChangeHandler?.let { target.focusChangeHandler = it }
    touchHandler?.let { target.touchHandler = it }
    interceptTouchHandler?.let { target.interceptTouchHandler = it }
    // Copy after clickHandler so the clickHandler setter's "clear additives" side effect doesn't
    // wipe out the additives we just propagated.
    extraClickListeners?.let { target.extraClickListeners = ArrayList(it) }

    if (isScaleSet) target.scale = scale
    if (isAlphaSet) target.alpha = alpha
    if (isRotationSet) target.rotation = rotation
    if (isRotationXSet) target.rotationX = rotationX
    if (isRotationYSet) target.rotationY = rotationY
    if (isClipChildrenSet) target.clipChildren = clipChildren
    if (isDefaultFocusHighlightEnabledSet)
        target.defaultFocusHighlightEnabled = defaultFocusHighlightEnabled
    if (flags and FLAG_CLIP_TO_OUTLINE != 0) target.clipToOutline = clipToOutline
    if (flags and FLAG_SHADOW_ELEVATION != 0) target.shadowElevation = shadowElevation
    if (flags and FLAG_AMBIENT_SHADOW_COLOR != 0) target.ambientShadowColor = ambientShadowColor
    if (flags and FLAG_SPOT_SHADOW_COLOR != 0) target.spotShadowColor = spotShadowColor
    if (focusState != NodeInfo.FOCUS_UNSET) target.focusState = focusState
    if (clickableState != NodeInfo.CLICKABLE_UNSET) target.clickableState = clickableState
    if (enabledState != NodeInfo.ENABLED_UNSET) target.enabledState = enabledState
    if (selectedState != NodeInfo.SELECTED_UNSET) target.selectedState = selectedState
    if (keyboardNavigationClusterState != NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_UNSET)
        target.keyboardNavigationClusterState = keyboardNavigationClusterState
    visibility?.let { target.visibility = it }
  }

  fun copyInto(target: ViewAttributes) {
    tooltipText?.let { target.tooltipText = it }
    if (hasViewId()) {
      target.viewId = viewId
    }
    viewTag?.let { target.viewTag = it }
    viewTags?.let { target.addViewTags(it) }
    transitionName?.let { target.transitionName = it }
    outlineProvider?.let { target.outlineProvider = it }
    renderEffect?.let { target.renderEffect = it }

    clickHandler?.let { target.clickHandler = it }
    longClickHandler?.let { target.longClickHandler = it }
    focusChangeHandler?.let { target.focusChangeHandler = it }
    touchHandler?.let { target.touchHandler = it }
    interceptTouchHandler?.let { target.interceptTouchHandler = it }
    // Copy after clickHandler so the clickHandler setter's "clear additives" side effect doesn't
    // wipe out the additives we just propagated.
    extraClickListeners?.let { target.extraClickListeners = ArrayList(it) }

    if (isScaleSet) target.scale = scale
    if (isAlphaSet) target.alpha = alpha
    if (isRotationSet) target.rotation = rotation
    if (isRotationXSet) target.rotationX = rotationX
    if (isRotationYSet) target.rotationY = rotationY
    if (isClipChildrenSet) target.clipChildren = clipChildren
    if (isDefaultFocusHighlightEnabledSet)
        target.defaultFocusHighlightEnabled = defaultFocusHighlightEnabled
    if (flags and FLAG_CLIP_TO_OUTLINE != 0) target.clipToOutline = clipToOutline
    if (flags and FLAG_SHADOW_ELEVATION != 0) target.shadowElevation = shadowElevation
    if (flags and FLAG_AMBIENT_SHADOW_COLOR != 0) target.ambientShadowColor = ambientShadowColor
    if (flags and FLAG_SPOT_SHADOW_COLOR != 0) target.spotShadowColor = spotShadowColor
    if (focusState != NodeInfo.FOCUS_UNSET) {
      target.isFocusable = focusState == NodeInfo.FOCUS_SET_TRUE
    }
    if (clickableState != NodeInfo.CLICKABLE_UNSET) {
      target.isClickable = clickableState == NodeInfo.CLICKABLE_SET_TRUE
    }
    if (enabledState != NodeInfo.ENABLED_UNSET) {
      target.isEnabled = enabledState == NodeInfo.ENABLED_SET_TRUE
    }
    if (selectedState != NodeInfo.SELECTED_UNSET) {
      target.isSelected = selectedState == NodeInfo.SELECTED_SET_TRUE
    }
    if (keyboardNavigationClusterState != NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_UNSET) {
      target.isKeyboardNavigationCluster =
          keyboardNavigationClusterState == NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_TRUE
    }
    visibility?.let { target.visibility = it }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BinderMigratedNodeInfo) return false

    if (flags != other.flags) return false
    if (alpha != other.alpha) return false
    if (clipToOutline != other.clipToOutline) return false
    if (clipChildren != other.clipChildren) return false
    if (defaultFocusHighlightEnabled != other.defaultFocusHighlightEnabled) return false
    if (!equals(tooltipText, other.tooltipText)) return false
    if (!equals(outlineProvider, other.outlineProvider)) return false
    if (!equals(renderEffect, other.renderEffect)) return false
    if (rotation != other.rotation) return false
    if (rotationX != other.rotationX) return false
    if (rotationY != other.rotationY) return false
    if (scale != other.scale) return false
    if (shadowElevation != other.shadowElevation) return false
    if (ambientShadowColor != other.ambientShadowColor) return false
    if (spotShadowColor != other.spotShadowColor) return false
    if (viewId != other.viewId) return false
    if (!equals(viewTag, other.viewTag)) return false
    if (!equals(viewTags, other.viewTags)) return false

    if (transitionName != other.transitionName) return false

    if (!isEquivalentTo(clickHandler, other.clickHandler)) return false
    if (!isEquivalentTo(longClickHandler, other.longClickHandler)) return false
    if (!isEquivalentTo(focusChangeHandler, other.focusChangeHandler)) return false
    if (!isEquivalentTo(touchHandler, other.touchHandler)) return false
    if (!isEquivalentTo(interceptTouchHandler, other.interceptTouchHandler)) return false
    // Additive listener identity matters for equivalence — two nodes with the same primary but
    // different additives are not equivalent.
    if (extraClickListeners != other.extraClickListeners) return false

    if (focusState != other.focusState) return false
    if (clickableState != other.clickableState) return false
    if (enabledState != other.enabledState) return false
    if (selectedState != other.selectedState) return false
    if (keyboardNavigationClusterState != other.keyboardNavigationClusterState) return false
    if (visibility != other.visibility) return false

    return true
  }

  override fun hashCode(): Int {
    var result = viewId.hashCode()
    result = 31 * result + (viewTag?.hashCode() ?: 0)
    result = 31 * result + (transitionName?.hashCode() ?: 0)
    result = 31 * result + (viewTags?.hashCode() ?: 0)
    result = 31 * result + (outlineProvider?.hashCode() ?: 0)
    result = 31 * result + (renderEffect?.hashCode() ?: 0)
    result = 31 * result + flags
    result = 31 * result + scale.hashCode()
    result = 31 * result + alpha.hashCode()
    result = 31 * result + rotation.hashCode()
    result = 31 * result + rotationX.hashCode()
    result = 31 * result + rotationY.hashCode()
    result = 31 * result + clipChildren.hashCode()
    result = 31 * result + defaultFocusHighlightEnabled.hashCode()
    result = 31 * result + clipToOutline.hashCode()
    result = 31 * result + shadowElevation.hashCode()
    result = 31 * result + ambientShadowColor
    result = 31 * result + spotShadowColor
    result = 31 * result + (tooltipText?.hashCode() ?: 0)
    result = 31 * result + (clickHandler?.hashCode() ?: 0)
    result = 31 * result + (longClickHandler?.hashCode() ?: 0)
    result = 31 * result + (focusChangeHandler?.hashCode() ?: 0)
    result = 31 * result + (touchHandler?.hashCode() ?: 0)
    result = 31 * result + (interceptTouchHandler?.hashCode() ?: 0)
    result = 31 * result + (extraClickListeners?.hashCode() ?: 0)
    result = 31 * result + focusState
    result = 31 * result + clickableState
    result = 31 * result + enabledState
    result = 31 * result + selectedState
    result = 31 * result + keyboardNavigationClusterState
    result = 31 * result + (visibility?.hashCode() ?: 0)
    return result
  }

  companion object {
    internal const val FLAG_SCALE = 1 shl 0
    internal const val FLAG_ALPHA = 1 shl 1
    internal const val FLAG_ROTATION = 1 shl 2
    internal const val FLAG_ROTATION_X = 1 shl 3
    internal const val FLAG_ROTATION_Y = 1 shl 4
    internal const val FLAG_CLIP_CHILDREN = 1 shl 5
    internal const val FLAG_CLIP_TO_OUTLINE = 1 shl 6
    internal const val FLAG_SHADOW_ELEVATION = 1 shl 7
    internal const val FLAG_AMBIENT_SHADOW_COLOR = 1 shl 8
    internal const val FLAG_SPOT_SHADOW_COLOR = 1 shl 9
    internal const val FLAG_VIEW_TAG = 1 shl 10
    internal const val FLAG_VIEW_ID = 1 shl 11
    internal const val FLAG_VIEW_TAGS = 1 shl 12
    internal const val FLAG_DEFAULT_FOCUS_HIGHLIGHT_ENABLED = 1 shl 13
  }
}
