// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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

import android.animation.StateListAnimator
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.facebook.litho.drawable.DrawableUtils
import com.facebook.rendercore.primitives.utils.equals
import com.facebook.rendercore.primitives.utils.isEquivalentTo
import com.facebook.yoga.YogaDirection

class ViewAttributes {

  var isHostSpec: Boolean = false
  var componentName: String = ""
  var importantForAccessibility: Int = 0
  var disableDrawableOutputs: Boolean = false

  var contentDescription: CharSequence? = null
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
  var viewTags: SparseArray<Any>? = null
  var outlineProvider: ViewOutlineProvider? = null
  var clickHandler: EventHandler<ClickEvent>? = null
  var longClickHandler: EventHandler<LongClickEvent>? = null
  var focusChangeHandler: EventHandler<FocusChangedEvent>? = null
  var touchHandler: EventHandler<TouchEvent>? = null
  var interceptTouchHandler: EventHandler<InterceptTouchEvent>? = null

  var background: Drawable? = null
  var foreground: Drawable? = null
  var padding: Rect? = null
  var layoutDirection: YogaDirection = YogaDirection.INHERIT
  var stateListAnimator: StateListAnimator? = null
  @DrawableRes var stateListAnimatorRes: Int = 0
  @LayerType var layerType: Int = LayerType.LAYER_TYPE_NOT_SET
  var layoutPaint: Paint? = null

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

  var clipToOutline: Boolean = false
    set(value) {
      field = value
      flags = flags or FLAG_CLIP_TO_OUTLINE
    }

  var isFocusable: Boolean = false
    set(value) {
      field = value
      flags = flags or FLAG_FOCUS
    }

  var isClickable: Boolean = false
    set(value) {
      field = value
      flags = flags or FLAG_CLICKABLE
    }

  var isEnabled: Boolean = false
    set(value) {
      field = value
      flags = flags or FLAG_ENABLED
    }

  var isSelected: Boolean = false
    set(value) {
      field = value
      flags = flags or FLAG_SELECTED
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

  val isClipChildrenSet: Boolean
    get() = flags and FLAG_CLIP_CHILDREN != 0

  val isFocusableSet: Boolean
    get() = flags and FLAG_FOCUS != 0

  val isClickableSet: Boolean
    get() = flags and FLAG_CLICKABLE != 0

  val isEnabledSet: Boolean
    get() = flags and FLAG_ENABLED != 0

  val isSelectedSet: Boolean
    get() = flags and FLAG_SELECTED != 0

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

  fun hasPadding(): Boolean = padding != null

  val paddingLeft: Int
    get() = padding?.left ?: 0

  val paddingTop: Int
    get() = padding?.top ?: 0

  val paddingRight: Int
    get() = padding?.right ?: 0

  val paddingBottom: Int
    get() = padding?.bottom ?: 0

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ViewAttributes) return false

    if (isHostSpec != other.isHostSpec) return false
    if (componentName != other.componentName) return false
    if (importantForAccessibility != other.importantForAccessibility) return false
    if (disableDrawableOutputs != other.disableDrawableOutputs) return false

    if (flags != other.flags) return false
    if (alpha != other.alpha) return false
    if (!isEquivalentTo(clickHandler, other.clickHandler)) return false
    if (clipToOutline != other.clipToOutline) return false
    if (clipChildren != other.clipChildren) return false
    if (!equals(contentDescription, other.contentDescription)) return false
    if (isEnabled != other.isEnabled) return false
    if (!isEquivalentTo(focusChangeHandler, other.focusChangeHandler)) return false
    if (isFocusable != other.isFocusable) return false
    if (!isEquivalentTo(interceptTouchHandler, other.interceptTouchHandler)) return false
    if (!isEquivalentTo(longClickHandler, other.longClickHandler)) return false
    if (!equals(outlineProvider, other.outlineProvider)) return false
    if (rotation != other.rotation) return false
    if (rotationX != other.rotationX) return false
    if (rotationY != other.rotationY) return false
    if (scale != other.scale) return false
    if (isSelected != other.isSelected) return false
    if (isClickable != other.isClickable) return false
    if (shadowElevation != other.shadowElevation) return false
    if (ambientShadowColor != other.ambientShadowColor) return false
    if (spotShadowColor != other.spotShadowColor) return false
    if (!isEquivalentTo(touchHandler, other.touchHandler)) return false
    if (viewId != other.viewId) return false
    if (!equals(viewTag, other.viewTag)) return false
    if (!equals(viewTags, other.viewTags)) return false

    if (!DrawableUtils.isEquivalentTo(background, other.background)) return false
    if (!DrawableUtils.isEquivalentTo(foreground, other.foreground)) return false
    if (!equals(padding, other.padding)) return false
    if (!equals(layoutDirection, other.layoutDirection)) return false
    if (stateListAnimatorRes != other.stateListAnimatorRes) return false
    // TODO: (T33421916) We need compare StateListAnimators more accurately
    if (!equals(stateListAnimator, other.stateListAnimator)) return false

    if (transitionName != other.transitionName) return false
    if (layerType != other.layerType) return false
    if (layoutPaint != other.layoutPaint) return false

    return true
  }

  override fun hashCode(): Int {
    var result = isHostSpec.hashCode()
    result = 31 * result + componentName.hashCode()
    result = 31 * result + importantForAccessibility
    result = 31 * result + disableDrawableOutputs.hashCode()
    result = 31 * result + (contentDescription?.hashCode() ?: 0)
    result = 31 * result + (viewId.hashCode())
    result = 31 * result + (viewTag?.hashCode() ?: 0)
    result = 31 * result + (transitionName?.hashCode() ?: 0)
    result = 31 * result + (viewTags?.hashCode() ?: 0)
    result = 31 * result + (outlineProvider?.hashCode() ?: 0)
    result = 31 * result + (clickHandler?.hashCode() ?: 0)
    result = 31 * result + (longClickHandler?.hashCode() ?: 0)
    result = 31 * result + (focusChangeHandler?.hashCode() ?: 0)
    result = 31 * result + (touchHandler?.hashCode() ?: 0)
    result = 31 * result + (interceptTouchHandler?.hashCode() ?: 0)
    result = 31 * result + (background?.hashCode() ?: 0)
    result = 31 * result + (foreground?.hashCode() ?: 0)
    result = 31 * result + (padding?.hashCode() ?: 0)
    result = 31 * result + layoutDirection.hashCode()
    result = 31 * result + (stateListAnimator?.hashCode() ?: 0)
    result = 31 * result + stateListAnimatorRes
    result = 31 * result + layerType
    result = 31 * result + (layoutPaint?.hashCode() ?: 0)
    result = 31 * result + flags
    result = 31 * result + scale.hashCode()
    result = 31 * result + alpha.hashCode()
    result = 31 * result + rotation.hashCode()
    result = 31 * result + rotationX.hashCode()
    result = 31 * result + rotationY.hashCode()
    result = 31 * result + clipChildren.hashCode()
    result = 31 * result + clipToOutline.hashCode()
    result = 31 * result + isFocusable.hashCode()
    result = 31 * result + isClickable.hashCode()
    result = 31 * result + isEnabled.hashCode()
    result = 31 * result + isSelected.hashCode()
    result = 31 * result + shadowElevation.hashCode()
    result = 31 * result + ambientShadowColor
    result = 31 * result + spotShadowColor
    return result
  }

  fun copyInto(target: ViewAttributes) {
    target.isHostSpec = isHostSpec
    target.componentName = componentName
    target.importantForAccessibility = importantForAccessibility
    target.disableDrawableOutputs = disableDrawableOutputs

    contentDescription?.let { target.contentDescription = it }
    target.viewId = viewId
    viewTag?.let { target.viewTag = it }
    transitionName?.let { target.transitionName = it }
    viewTags?.let { target.viewTags = it }
    outlineProvider?.let { target.outlineProvider = it }
    clickHandler?.let { target.clickHandler = it }
    longClickHandler?.let { target.longClickHandler = it }
    focusChangeHandler?.let { target.focusChangeHandler = it }
    touchHandler?.let { target.touchHandler = it }
    interceptTouchHandler?.let { target.interceptTouchHandler = it }

    background?.let { target.background = it }
    foreground?.let { target.foreground = it }
    padding?.let { target.padding = it }
    target.layoutDirection = layoutDirection
    stateListAnimator?.let { target.stateListAnimator = it }
    target.stateListAnimatorRes = stateListAnimatorRes
    target.layerType = layerType
    layoutPaint?.let { target.layoutPaint = it }

    if (isScaleSet) target.scale = scale
    if (isAlphaSet) target.alpha = alpha
    if (isRotationSet) target.rotation = rotation
    if (isRotationXSet) target.rotationX = rotationX
    if (isRotationYSet) target.rotationY = rotationY
    if (isClipChildrenSet) target.clipChildren = clipChildren
    target.clipToOutline = clipToOutline
    if (isFocusableSet) target.isFocusable = isFocusable
    if (isClickableSet) target.isClickable = isClickable
    if (isEnabledSet) target.isEnabled = isEnabled
    if (isSelectedSet) target.isSelected = isSelected
    if (flags and FLAG_SHADOW_ELEVATION != 0) target.shadowElevation = shadowElevation
    if (flags and FLAG_AMBIENT_SHADOW_COLOR != 0) target.ambientShadowColor = ambientShadowColor
    if (flags and FLAG_SPOT_SHADOW_COLOR != 0) target.spotShadowColor = spotShadowColor
  }

  companion object {
    private const val FLAG_SCALE = 1 shl 1
    private const val FLAG_ALPHA = 1 shl 2
    private const val FLAG_ROTATION = 1 shl 3
    private const val FLAG_ROTATION_X = 1 shl 4
    private const val FLAG_ROTATION_Y = 1 shl 5

    private const val FLAG_CLIP_CHILDREN = 1 shl 6
    private const val FLAG_CLIP_TO_OUTLINE = 1 shl 7
    private const val FLAG_FOCUS = 1 shl 8
    private const val FLAG_CLICKABLE = 1 shl 9
    private const val FLAG_ENABLED = 1 shl 10
    private const val FLAG_SELECTED = 1 shl 11

    private const val FLAG_SHADOW_ELEVATION = 1 shl 12
    private const val FLAG_AMBIENT_SHADOW_COLOR = 1 shl 13
    private const val FLAG_SPOT_SHADOW_COLOR = 1 shl 14

    private const val FLAG_VIEW_TAG = 1 shl 15
    private const val FLAG_VIEW_ID = 1 shl 16
  }
}
