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

@file:JvmName("ViewAttributes")

package com.facebook.litho

import android.animation.AnimatorInflater
import android.animation.StateListAnimator
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import com.facebook.litho.drawable.DrawableUtils
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.utils.VersionedAndroidApis
import com.facebook.litho.visibility.Visibility
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.utils.equals
import com.facebook.rendercore.utils.isEquivalentTo

class ViewAttributes {

  var isHostSpec: Boolean = false
  var componentName: String = ""
  var importantForAccessibility: Int = 0
  var disableDrawableOutputs: Boolean = false

  var contentDescription: CharSequence? = null
  var accessibilityPaneTitle: CharSequence? = null
  var liveRegionMode: Int? = null
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
  var longClickHandler: EventHandler<LongClickEvent>? = null
  var focusChangeHandler: EventHandler<FocusChangedEvent>? = null
  var touchHandler: EventHandler<TouchEvent>? = null
  var interceptTouchHandler: EventHandler<InterceptTouchEvent>? = null

  var background: Drawable? = null
  var foreground: Drawable? = null
  var layoutDirection: LayoutDirection = LayoutDirection.INHERIT
  var stateListAnimator: StateListAnimator? = null
  @DrawableRes var stateListAnimatorRes: Int = 0
  @LayerType var layerType: Int = LayerType.LAYER_TYPE_NOT_SET
  var layoutPaint: Paint? = null
  var visibility: Visibility? = null

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

  var isKeyboardNavigationCluster: Boolean = false
    set(value) {
      field = value
      flags = flags or FLAG_KEYBOARD_NAVIGATION_CLUSTER
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

  val isKeyboardNavigationClusterSet: Boolean
    get() = flags and FLAG_KEYBOARD_NAVIGATION_CLUSTER != 0

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

  var systemGestureExclusionZones: List<(Rect) -> Rect>? = null

  var viewTags: SparseArray<Any>? = null

  fun addViewTags(viewTags: SparseArray<Any>?) {
    if (this.viewTags == null) {
      this.viewTags = viewTags
    } else {
      this.viewTags = CollectionsUtils.mergeSparseArrays(this.viewTags, viewTags)
    }
  }

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
    if (!equals(accessibilityPaneTitle, other.accessibilityPaneTitle)) return false
    if (!equals(liveRegionMode, other.liveRegionMode)) return false
    if (!equals(tooltipText, other.tooltipText)) return false
    if (isEnabled != other.isEnabled) return false
    if (!isEquivalentTo(focusChangeHandler, other.focusChangeHandler)) return false
    if (isFocusable != other.isFocusable) return false
    if (!isEquivalentTo(interceptTouchHandler, other.interceptTouchHandler)) return false
    if (!isEquivalentTo(longClickHandler, other.longClickHandler)) return false
    if (!equals(outlineProvider, other.outlineProvider)) return false
    if (!equals(renderEffect, other.renderEffect)) return false
    if (rotation != other.rotation) return false
    if (rotationX != other.rotationX) return false
    if (rotationY != other.rotationY) return false
    if (scale != other.scale) return false
    if (isSelected != other.isSelected) return false
    if (isKeyboardNavigationCluster != other.isKeyboardNavigationCluster) return false
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
    result = 31 * result + (accessibilityPaneTitle?.hashCode() ?: 0)
    result = 31 * result + (liveRegionMode?.hashCode() ?: 0)
    result = 31 * result + (viewId.hashCode())
    result = 31 * result + (viewTag?.hashCode() ?: 0)
    result = 31 * result + (transitionName?.hashCode() ?: 0)
    result = 31 * result + (viewTags?.hashCode() ?: 0)
    result = 31 * result + (outlineProvider?.hashCode() ?: 0)
    result = 31 * result + (renderEffect?.hashCode() ?: 0)
    result = 31 * result + (clickHandler?.hashCode() ?: 0)
    result = 31 * result + (longClickHandler?.hashCode() ?: 0)
    result = 31 * result + (focusChangeHandler?.hashCode() ?: 0)
    result = 31 * result + (touchHandler?.hashCode() ?: 0)
    result = 31 * result + (interceptTouchHandler?.hashCode() ?: 0)
    result = 31 * result + (background?.hashCode() ?: 0)
    result = 31 * result + (foreground?.hashCode() ?: 0)
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
    result = 31 * result + isKeyboardNavigationCluster.hashCode()
    result = 31 * result + shadowElevation.hashCode()
    result = 31 * result + ambientShadowColor
    result = 31 * result + spotShadowColor
    result = 31 * result + (tooltipText?.hashCode() ?: 0)
    return result
  }

  fun copyInto(target: ViewAttributes) {
    target.isHostSpec = isHostSpec
    target.componentName = componentName
    target.importantForAccessibility = importantForAccessibility
    target.disableDrawableOutputs = disableDrawableOutputs

    contentDescription?.let { target.contentDescription = it }
    accessibilityPaneTitle?.let { target.accessibilityPaneTitle = it }
    liveRegionMode?.let { target.liveRegionMode = it }
    tooltipText?.let { target.tooltipText = it }
    target.viewId = viewId
    viewTag?.let { target.viewTag = it }
    transitionName?.let { target.transitionName = it }
    viewTags?.let { target.viewTags = it }
    outlineProvider?.let { target.outlineProvider = it }
    renderEffect?.let { target.renderEffect = it }
    clickHandler?.let { target.clickHandler = it }
    longClickHandler?.let { target.longClickHandler = it }
    focusChangeHandler?.let { target.focusChangeHandler = it }
    touchHandler?.let { target.touchHandler = it }
    interceptTouchHandler?.let { target.interceptTouchHandler = it }

    background?.let { target.background = it }
    foreground?.let { target.foreground = it }
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
    if (isKeyboardNavigationClusterSet)
        target.isKeyboardNavigationCluster = isKeyboardNavigationCluster
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

    private const val FLAG_KEYBOARD_NAVIGATION_CLUSTER = 1 shl 17

    @JvmStatic
    fun setViewAttributes(
        content: Any?,
        attributes: ViewAttributes,
        unit: RenderUnit<*>?,
        cloneStateListAnimators: Boolean = false
    ) {
      if (content !is View) {
        return
      }

      if (content is ComponentHost) {
        content.setSafeViewModificationsEnabled(true)
      }

      attributes.visibility?.let { content.visibility = it.toViewVisibility() }

      setClickHandler(attributes.clickHandler, content)
      setLongClickHandler(attributes.longClickHandler, content)
      setFocusChangeHandler(attributes.focusChangeHandler, content)
      setTouchHandler(attributes.touchHandler, content)
      setInterceptTouchHandler(attributes.interceptTouchHandler, content)
      if (unit is LithoRenderUnit) {
        val nodeInfo = unit.nodeInfo
        if (nodeInfo != null) setAccessibilityDelegate(content, nodeInfo)
      }
      setViewId(content, attributes.viewId)
      if (attributes.isTagSet) {
        setViewTag(content, attributes.viewTag)
      }
      addViewTags(content, attributes.viewTags)
      setShadowElevation(content, attributes.shadowElevation)
      setAmbientShadowColor(content, attributes.ambientShadowColor)
      setSpotShadowColor(content, attributes.spotShadowColor)
      setOutlineProvider(content, attributes.outlineProvider)
      setRenderEffect(content, attributes.renderEffect)
      setClipToOutline(content, attributes.clipToOutline)
      setClipChildren(content, attributes)
      setContentDescription(content, attributes.contentDescription)
      setPaneTitle(content, attributes.accessibilityPaneTitle)
      setLiveRegion(content, attributes.liveRegionMode)
      setFocusable(content, attributes)
      setClickable(content, attributes)
      setEnabled(content, attributes)
      setSelected(content, attributes)
      setKeyboardNavigationCluster(content, attributes)
      setTooltipText(content, attributes.tooltipText)
      setScale(content, attributes)
      setAlpha(content, attributes)
      setRotation(content, attributes)
      setRotationX(content, attributes)
      setRotationY(content, attributes)
      setTransitionName(content, attributes.transitionName)
      setImportantForAccessibility(content, attributes.importantForAccessibility)
      val isHostSpec = attributes.isHostSpec
      setViewLayerType(content, attributes)
      setViewStateListAnimator(content, attributes, cloneStateListAnimators)
      if (attributes.disableDrawableOutputs) {
        setViewBackground(content, attributes)
        setViewForeground(content, attributes)

        // when background outputs are disabled, they are wrapped by a ComponentHost.
        // A background can set the padding of a view, but ComponentHost should not have
        // any padding because the layout calculation has already accounted for padding by
        // translating the bounds of its children.
        if (isHostSpec) {
          content.setPadding(0, 0, 0, 0)
        }
      }
      if (!isHostSpec) {
        // Set view background, if applicable.  Do this before padding
        // as it otherwise overrides the padding.
        setViewBackground(content, attributes)
        setViewForeground(content, attributes)
        setViewLayoutDirection(content, attributes)
      }

      if (content is ComponentHost) {
        content.setSafeViewModificationsEnabled(false)
      }
    }

    private fun addViewTags(view: View, viewTags: SparseArray<Any>?) {
      if (viewTags == null) {
        return
      }
      if (view is ComponentHost) {
        view.addViewTags(viewTags)
      } else {
        for (i in 0 until viewTags.size()) {
          view.setTag(viewTags.keyAt(i), viewTags.valueAt(i))
        }
      }
    }

    @JvmStatic
    fun unsetViewAttributes(content: Any?, attributes: ViewAttributes, mountFlags: Int) {
      val isHostView = attributes.isHostSpec
      if (content !is View) {
        return
      }

      if (content is ComponentHost) {
        content.setSafeViewModificationsEnabled(true)
      }

      // If visibility was set the unset it back to the original value.
      attributes.visibility?.let {
        content.visibility = LithoMountData.getOriginalVisibility(mountFlags)
      }

      if (attributes.clickHandler != null) {
        unsetClickHandler(content)
      }
      if (attributes.longClickHandler != null) {
        unsetLongClickHandler(content)
      }
      if (attributes.focusChangeHandler != null) {
        unsetFocusChangeHandler(content)
      }
      if (attributes.touchHandler != null) {
        unsetTouchHandler(content)
      }
      if (attributes.interceptTouchHandler != null) {
        unsetInterceptTouchEventHandler(content)
      }
      if (attributes.isViewIdSet) {
        unsetViewId(content)
      }
      if (attributes.isTagSet) {
        unsetViewTag(content)
      }
      unsetViewTags(content, attributes.viewTags)
      // unset the state list animator before any other draw properties as jumping it to its end
      // state could mutate other view properties
      unsetViewStateListAnimator(content, attributes)
      unsetShadowElevation(content, attributes.shadowElevation)
      unsetAmbientShadowColor(content, attributes.ambientShadowColor)
      unsetSpotShadowColor(content, attributes.spotShadowColor)
      unsetOutlineProvider(content, attributes.outlineProvider)
      unsetRenderEffect(content, attributes.renderEffect)
      unsetClipToOutline(content, attributes.clipToOutline)
      unsetClipChildren(content, attributes.clipChildren)
      if (!attributes.contentDescription.isNullOrEmpty()) {
        unsetContentDescription(content)
      }
      if (!attributes.accessibilityPaneTitle.isNullOrEmpty()) {
        unsetPaneTitle(content)
      }
      if (attributes.liveRegionMode != null) {
        unsetLiveRegion(content)
      }
      if (!attributes.tooltipText.isNullOrEmpty()) {
        unsetTooltipText(content)
      }
      unsetScale(content, attributes)
      unsetAlpha(content, attributes)
      unsetRotation(content, attributes)
      unsetRotationX(content, attributes)
      unsetRotationY(content, attributes)
      content.isClickable = LithoMountData.isViewClickable(mountFlags)
      content.isLongClickable = LithoMountData.isViewLongClickable(mountFlags)
      unsetFocusable(content, mountFlags)
      unsetEnabled(content, mountFlags)
      unsetSelected(content, mountFlags)
      unsetKeyboardNavigationCluster(content, mountFlags)
      if (attributes.importantForAccessibility != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
        unsetImportantForAccessibility(content)
      }
      unsetAccessibilityDelegate(content)
      // Host view doesn't set its own padding, but gets absolute positions for inner content from
      // Yoga. Also bg/fg is used as separate drawables instead of using View's bg/fg attribute.
      if (attributes.disableDrawableOutputs) {
        unsetViewBackground(content, attributes)
        unsetViewForeground(content, attributes)
      }
      if (!isHostView) {
        unsetViewBackground(content, attributes)
        unsetViewForeground(content, attributes)
        unsetViewLayoutDirection(content)
      }
      unsetViewLayerType(content, mountFlags)

      if (content is ComponentHost) {
        content.setSafeViewModificationsEnabled(false)
      }

      if (!attributes.systemGestureExclusionZones.isNullOrEmpty()) {
        ViewCompat.setSystemGestureExclusionRects(content, emptyList())
      }
    }

    /**
     * Store a [NodeInfo] as a tag in `view`. [LithoView] contains the logic for setting/unsetting
     * it whenever accessibility is enabled/disabled
     *
     * For non [ComponentHost]s this is only done if any [EventHandler]s for accessibility events
     * have been implemented, we want to preserve the original behaviour since `view` might have had
     * a default delegate.
     */
    private fun setAccessibilityDelegate(view: View, nodeInfo: NodeInfo) {
      if (view !is ComponentHost && !nodeInfo.needsAccessibilityDelegate()) {
        return
      }
      view.setTag(ComponentHost.COMPONENT_NODE_INFO_ID, nodeInfo)
    }

    fun unsetAccessibilityDelegate(view: View) {
      if (view !is ComponentHost && view.getTag(ComponentHost.COMPONENT_NODE_INFO_ID) == null) {
        return
      }
      view.setTag(ComponentHost.COMPONENT_NODE_INFO_ID, null)
      if (view !is ComponentHost) {
        ViewCompat.setAccessibilityDelegate(view, null)
      }
    }

    /**
     * Installs the click listeners that will dispatch the click handler defined in the component's
     * props. Unconditionally set the clickable flag on the view.
     */
    private fun setClickHandler(clickHandler: EventHandler<ClickEvent>?, view: View) {
      if (clickHandler == null) {
        return
      }
      view.setOnClickListener(ComponentClickListener(clickHandler))
      view.isClickable = true
    }

    fun unsetClickHandler(view: View) {
      view.setOnClickListener(null)
      view.isClickable = false
    }

    /**
     * Installs the long click listeners that will dispatch the click handler defined in the
     * component's props. Unconditionally set the clickable flag on the view.
     */
    private fun setLongClickHandler(longClickHandler: EventHandler<LongClickEvent>?, view: View) {
      if (longClickHandler != null) {
        var listener = getComponentLongClickListener(view)
        if (listener == null) {
          listener = ComponentLongClickListener()
          setComponentLongClickListener(view, listener)
        }
        listener.eventHandler = longClickHandler
        view.isLongClickable = true
      }
    }

    fun unsetLongClickHandler(view: View) {
      val listener = getComponentLongClickListener(view)
      if (listener != null) {
        listener.eventHandler = null
      }
    }

    @JvmStatic
    fun getComponentLongClickListener(v: View): ComponentLongClickListener? =
        if (v is ComponentHost) {
          v.componentLongClickListener
        } else {
          v.getTag(R.id.component_long_click_listener) as? ComponentLongClickListener
        }

    @JvmStatic
    fun setComponentLongClickListener(v: View, listener: ComponentLongClickListener?) {
      if (v is ComponentHost) {
        v.componentLongClickListener = listener
      } else {
        v.setOnLongClickListener(listener)
        v.setTag(R.id.component_long_click_listener, listener)
      }
    }

    /**
     * Installs the on focus change listeners that will dispatch the click handler defined in the
     * component's props. Unconditionally set the clickable flag on the view.
     */
    private fun setFocusChangeHandler(
        focusChangeHandler: EventHandler<FocusChangedEvent>?,
        view: View
    ) {
      if (focusChangeHandler == null) {
        return
      }
      var listener = getComponentFocusChangeListener(view)
      if (listener == null) {
        listener = ComponentFocusChangeListener()
        setComponentFocusChangeListener(view, listener)
      }
      listener.eventHandler = focusChangeHandler
    }

    fun unsetFocusChangeHandler(view: View) {
      val listener = getComponentFocusChangeListener(view)
      if (listener != null) {
        listener.eventHandler = null
      }
    }

    @JvmStatic
    fun getComponentFocusChangeListener(v: View): ComponentFocusChangeListener? =
        if (v is ComponentHost) {
          v.componentFocusChangeListener
        } else {
          v.getTag(R.id.component_focus_change_listener) as? ComponentFocusChangeListener
        }

    @JvmStatic
    fun setComponentFocusChangeListener(v: View, listener: ComponentFocusChangeListener?) {
      if (v is ComponentHost) {
        v.componentFocusChangeListener = listener
      } else {
        v.onFocusChangeListener = listener
        v.setTag(R.id.component_focus_change_listener, listener)
      }
    }

    /**
     * Installs the touch listeners that will dispatch the touch handler defined in the component's
     * props.
     */
    private fun setTouchHandler(touchHandler: EventHandler<TouchEvent>?, view: View) {
      if (touchHandler != null) {
        var listener = getComponentTouchListener(view)
        if (listener == null) {
          listener = ComponentTouchListener()
          setComponentTouchListener(view, listener)
        }
        listener.eventHandler = touchHandler
      }
    }

    fun unsetTouchHandler(view: View) {
      val listener = getComponentTouchListener(view)
      if (listener != null) {
        listener.eventHandler = null
      }
    }

    /** Sets the intercept touch handler defined in the component's props. */
    private fun setInterceptTouchHandler(
        interceptTouchHandler: EventHandler<InterceptTouchEvent>?,
        view: View
    ) {
      if (interceptTouchHandler == null) {
        return
      }
      if (view is ComponentHost) {
        view.onInterceptTouchEventHandler = interceptTouchHandler
      }
    }

    fun unsetInterceptTouchEventHandler(view: View) {
      if (view is ComponentHost) {
        view.onInterceptTouchEventHandler = null
      }
    }

    @JvmStatic
    fun getComponentTouchListener(v: View): ComponentTouchListener? =
        if (v is ComponentHost) {
          v.componentTouchListener
        } else {
          v.getTag(R.id.component_touch_listener) as? ComponentTouchListener
        }

    @JvmStatic
    fun setComponentTouchListener(v: View, listener: ComponentTouchListener?) {
      if (v is ComponentHost) {
        v.componentTouchListener = listener
      } else {
        v.setOnTouchListener(listener)
        v.setTag(R.id.component_touch_listener, listener)
      }
    }

    private fun setViewId(view: View, @IdRes id: Int) {
      if (id != View.NO_ID) {
        view.id = id
      }
    }

    fun unsetViewId(view: View) {
      view.id = View.NO_ID
    }

    private fun setViewTag(view: View, viewTag: Any?) {
      view.tag = viewTag
    }

    fun unsetViewTag(view: View) {
      view.tag = null
    }

    fun unsetViewTags(view: View, viewTags: SparseArray<Any>?) {
      if (view is ComponentHost) {
        view.unsetViewTags()
      } else {
        if (viewTags != null) {
          for (i in 0 until viewTags.size()) {
            view.setTag(viewTags.keyAt(i), null)
          }
        }
      }
    }

    private fun setShadowElevation(view: View, shadowElevation: Float) {
      if (shadowElevation != 0f) {
        ViewCompat.setElevation(view, shadowElevation)
      }
    }

    private fun setAmbientShadowColor(view: View, @ColorInt ambientShadowColor: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        VersionedAndroidApis.P.setAmbientShadowColor(view, ambientShadowColor)
      }
    }

    private fun setSpotShadowColor(view: View, @ColorInt spotShadowColor: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        VersionedAndroidApis.P.setSpotShadowColor(view, spotShadowColor)
      }
    }

    private fun unsetShadowElevation(view: View, shadowElevation: Float) {
      if (shadowElevation != 0f) {
        ViewCompat.setElevation(view, 0f)
      }
    }

    fun unsetAmbientShadowColor(view: View, @ColorInt ambientShadowColor: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ambientShadowColor != Color.BLACK) {
        // Android documentation says black is the default:
        // https://developer.android.com/reference/android/view/View#getOutlineAmbientShadowColor()
        VersionedAndroidApis.P.setAmbientShadowColor(view, Color.BLACK)
      }
    }

    fun unsetSpotShadowColor(view: View, @ColorInt spotShadowColor: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && spotShadowColor != Color.BLACK) {
        // Android documentation says black is the default:
        // https://developer.android.com/reference/android/view/View#getOutlineSpotShadowColor()
        VersionedAndroidApis.P.setSpotShadowColor(view, Color.BLACK)
      }
    }

    private fun setOutlineProvider(view: View, outlineProvider: ViewOutlineProvider?) {
      if (outlineProvider != null) {
        view.outlineProvider = outlineProvider
      }
    }

    private fun unsetOutlineProvider(view: View, outlineProvider: ViewOutlineProvider?) {
      if (outlineProvider != null) {
        view.outlineProvider = ViewOutlineProvider.BACKGROUND
      }
    }

    private fun setRenderEffect(view: View, renderEffect: LithoRenderEffect?) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (renderEffect != null) {
          view.setRenderEffect(renderEffect.toRenderEffect())
        }
      }
    }

    private fun unsetRenderEffect(view: View, renderEffect: LithoRenderEffect?) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (renderEffect != null) {
          view.setRenderEffect(null)
        }
      }
    }

    private fun setClipToOutline(view: View, clipToOutline: Boolean) {
      if (clipToOutline) {
        view.clipToOutline = clipToOutline
      }
    }

    private fun unsetClipToOutline(view: View, clipToOutline: Boolean) {
      if (clipToOutline) {
        view.clipToOutline = false
      }
    }

    private fun setClipChildren(view: View, attributes: ViewAttributes) {
      if (attributes.isClipChildrenSet && view is ViewGroup) {
        view.clipChildren = attributes.clipChildren
      }
    }

    private fun unsetClipChildren(view: View, clipChildren: Boolean) {
      if (!clipChildren && view is ViewGroup) {
        // Default value for clipChildren is 'true'.
        // If this ViewGroup had clipChildren set to 'false' before mounting we would reset this
        // property here on recycling.
        view.clipChildren = true
      }
    }

    private fun setContentDescription(view: View, contentDescription: CharSequence?) {
      if (contentDescription.isNullOrEmpty()) {
        return
      }
      view.contentDescription = contentDescription
    }

    fun unsetContentDescription(view: View) {
      view.contentDescription = null
    }

    private fun setPaneTitle(view: View, accessibilityPaneTitle: CharSequence?) {
      if (accessibilityPaneTitle.isNullOrEmpty()) {
        return
      }
      ViewCompat.setAccessibilityPaneTitle(view, accessibilityPaneTitle)
    }

    fun unsetPaneTitle(view: View) {
      ViewCompat.setAccessibilityPaneTitle(view, null)
    }

    private fun setLiveRegion(view: View, liveRegionMode: Int?) {
      if (liveRegionMode == null) {
        return
      }
      view.accessibilityLiveRegion = liveRegionMode
    }

    private fun unsetLiveRegion(view: View) {
      view.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE
    }

    private fun setImportantForAccessibility(view: View, importantForAccessibility: Int) {
      if (importantForAccessibility == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
        return
      }
      ViewCompat.setImportantForAccessibility(view, importantForAccessibility)
    }

    private fun unsetImportantForAccessibility(view: View) {
      ViewCompat.setImportantForAccessibility(view, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    }

    private fun setFocusable(view: View, attributes: ViewAttributes) {
      if (attributes.isFocusableSet) {
        view.isFocusable = attributes.isFocusable
      }
    }

    fun unsetFocusable(view: View, flags: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        view.focusable = LithoMountData.getViewFocusable(flags)
      } else {
        view.isFocusable = LithoMountData.isViewFocusable(flags)
      }
    }

    private fun setTransitionName(view: View, transitionName: String?) {
      if (transitionName != null) {
        ViewCompat.setTransitionName(view, transitionName)
      }
    }

    private fun setClickable(view: View, attributes: ViewAttributes) {
      if (attributes.isClickableSet) {
        view.isClickable = attributes.isClickable
      }
    }

    private fun setEnabled(view: View, attributes: ViewAttributes) {
      if (attributes.isEnabledSet) {
        view.isEnabled = attributes.isEnabled
      }
    }

    private fun unsetEnabled(view: View, flags: Int) {
      view.isEnabled = LithoMountData.isViewEnabled(flags)
    }

    private fun setSelected(view: View, attributes: ViewAttributes) {
      if (attributes.isSelectedSet) {
        view.isSelected = attributes.isSelected
      }
    }

    private fun unsetSelected(view: View, flags: Int) {
      view.isSelected = LithoMountData.isViewSelected(flags)
    }

    private fun setKeyboardNavigationCluster(view: View, attributes: ViewAttributes) {
      if (attributes.isKeyboardNavigationClusterSet) {
        ViewCompat.setKeyboardNavigationCluster(view, attributes.isKeyboardNavigationCluster)
      }
    }

    private fun unsetKeyboardNavigationCluster(view: View, flags: Int) {
      ViewCompat.setKeyboardNavigationCluster(
          view, LithoMountData.isViewKeyboardNavigationCluster(flags))
    }

    private fun setTooltipText(view: View, tooltipText: String?) {
      ViewCompat.setTooltipText(view, tooltipText)
    }

    fun unsetTooltipText(view: View) {
      ViewCompat.setTooltipText(view, null)
    }

    private fun setScale(view: View, attributes: ViewAttributes) {
      if (attributes.isScaleSet) {
        val scale = attributes.scale
        view.scaleX = scale
        view.scaleY = scale
      }
    }

    private fun unsetScale(view: View, attributes: ViewAttributes) {
      if (attributes.isScaleSet) {
        if (view.scaleX != 1f) {
          view.scaleX = 1f
        }
        if (view.scaleY != 1f) {
          view.scaleY = 1f
        }
      }
    }

    private fun setAlpha(view: View, attributes: ViewAttributes) {
      if (attributes.isAlphaSet) {
        view.alpha = attributes.alpha
      }
    }

    private fun unsetAlpha(view: View, attributes: ViewAttributes) {
      if (attributes.isAlphaSet && view.alpha != 1f) {
        view.alpha = 1f
      }
    }

    private fun setRotation(view: View, attributes: ViewAttributes) {
      if (attributes.isRotationSet) {
        view.rotation = attributes.rotation
      }
    }

    private fun unsetRotation(view: View, attributes: ViewAttributes) {
      if (attributes.isRotationSet && view.rotation != 0f) {
        view.rotation = 0f
      }
    }

    private fun setRotationX(view: View, attributes: ViewAttributes) {
      if (attributes.isRotationXSet) {
        view.rotationX = attributes.rotationX
      }
    }

    private fun unsetRotationX(view: View, attributes: ViewAttributes) {
      if (attributes.isRotationXSet && view.rotationX != 0f) {
        view.rotationX = 0f
      }
    }

    private fun setRotationY(view: View, attributes: ViewAttributes) {
      if (attributes.isRotationYSet) {
        view.rotationY = attributes.rotationY
      }
    }

    private fun unsetRotationY(view: View, attributes: ViewAttributes) {
      if (attributes.isRotationYSet && view.rotationY != 0f) {
        view.rotationY = 0f
      }
    }

    private fun setViewBackground(view: View, attributes: ViewAttributes) {
      val background = attributes.background
      if (background != null) {
        setBackgroundCompat(view, background)
      }
    }

    private fun unsetViewBackground(view: View, attributes: ViewAttributes) {
      val background = attributes.background
      if (background != null) {
        setBackgroundCompat(view, null)
      }
    }

    @Suppress("deprecation")
    fun setBackgroundCompat(view: View, drawable: Drawable?) {
      view.background = drawable
    }

    private fun setViewForeground(view: View, attributes: ViewAttributes) {
      val foreground = attributes.foreground
      if (foreground != null) {
        view.foreground = foreground
      }
    }

    private fun unsetViewForeground(view: View, attributes: ViewAttributes) {
      val foreground = attributes.foreground
      if (foreground != null) {
        view.foreground = null
      }
    }

    private fun setViewLayoutDirection(view: View, attributes: ViewAttributes) {
      view.layoutDirection = attributes.layoutDirection.getLayoutDirectionForView()
    }

    fun unsetViewLayoutDirection(view: View) {
      view.layoutDirection = View.LAYOUT_DIRECTION_INHERIT
    }

    private fun setViewStateListAnimator(
        view: View,
        attributes: ViewAttributes,
        cloneStateListAnimators: Boolean
    ) {
      var stateListAnimator = attributes.stateListAnimator
      val stateListAnimatorRes = attributes.stateListAnimatorRes
      if (stateListAnimator == null && stateListAnimatorRes == 0) {
        return
      }
      if (stateListAnimator == null) {
        stateListAnimator =
            AnimatorInflater.loadStateListAnimator(view.context, stateListAnimatorRes)
      }
      if (cloneStateListAnimators) {
        stateListAnimator =
            try {
              stateListAnimator?.clone()
            } catch (e: CloneNotSupportedException) {
              // If we fail to clone, just fallback to the original animator
              stateListAnimator
            }
      }
      view.stateListAnimator = stateListAnimator
    }

    private fun unsetViewStateListAnimator(view: View, attributes: ViewAttributes) {
      if (attributes.stateListAnimator == null && attributes.stateListAnimatorRes == 0) {
        return
      }
      unsetViewStateListAnimator(view)
    }

    fun unsetViewStateListAnimator(view: View) {
      if (view.stateListAnimator != null) {
        view.stateListAnimator.jumpToCurrentState()
        view.stateListAnimator = null
      }
    }

    private fun setViewLayerType(view: View, attributes: ViewAttributes) {
      val type = attributes.layerType
      if (type != LayerType.LAYER_TYPE_NOT_SET) {
        view.setLayerType(attributes.layerType, attributes.layoutPaint)
      }
    }

    private fun unsetViewLayerType(view: View, mountFlags: Int) {
      val type = LithoMountData.getOriginalLayerType(mountFlags)
      if (type != LayerType.LAYER_TYPE_NOT_SET) {
        view.setLayerType(type, null)
      }
    }
  }
}
