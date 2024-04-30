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

import android.animation.AnimatorInflater
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import com.facebook.litho.LithoViewAttributesExtension.LithoViewAttributesState
import com.facebook.litho.LithoViewAttributesExtension.ViewAttributesInput
import com.facebook.rendercore.ErrorReporter
import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.MountState
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.OnItemCallbacks
import com.facebook.rendercore.utils.equals

class LithoViewAttributesExtension private constructor() :
    MountExtension<ViewAttributesInput, LithoViewAttributesState>(),
    OnItemCallbacks<LithoViewAttributesState> {

  interface ViewAttributesInput {
    val viewAttributes: Map<Long, ViewAttributes>?
  }

  override fun createState(): LithoViewAttributesState = LithoViewAttributesState()

  class LithoViewAttributesState {
    private val _defaultViewAttributes: MutableMap<Long, Int> = HashMap()
    internal var currentUnits: Map<Long, ViewAttributes>? = null
    internal var newUnits: Map<Long, ViewAttributes>? = null

    fun setDefaultViewAttributes(renderUnitId: Long, flags: Int) {
      _defaultViewAttributes[renderUnitId] = flags
    }

    fun getDefaultViewAttributes(renderUnitId: Long): Int {
      return _defaultViewAttributes[renderUnitId]
          ?: throw IllegalStateException(
              "View attributes not found, did you call onUnbindItem without onBindItem?")
    }

    fun hasDefaultViewAttributes(renderUnitId: Long): Boolean =
        _defaultViewAttributes.containsKey(renderUnitId)

    fun getCurrentViewAttributes(id: Long): ViewAttributes? = currentUnits?.get(id)

    fun getNewViewAttributes(id: Long): ViewAttributes? = newUnits?.get(id)
  }

  override fun beforeMount(
      extensionState: ExtensionState<LithoViewAttributesState>,
      input: ViewAttributesInput?,
      localVisibleRect: Rect?
  ) {
    if (input != null) {
      extensionState.state?.newUnits = input.viewAttributes
    }
  }

  override fun afterMount(extensionState: ExtensionState<LithoViewAttributesState>) {
    extensionState.state.currentUnits = extensionState.state.newUnits
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
      setViewAttributes(content, viewAttributes, renderUnit)
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
    extensionState.state.currentUnits = null
    extensionState.state.newUnits = null
  }

  companion object {
    @get:JvmStatic val instance: LithoViewAttributesExtension = LithoViewAttributesExtension()

    @JvmStatic
    fun setViewAttributes(content: Any?, attributes: ViewAttributes, unit: RenderUnit<*>?) {
      if (content !is View) {
        return
      }

      if (content is ComponentHost) {
        content.setSafeViewModificationsEnabled(true)
      }

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
      setViewTags(content, attributes.viewTags)
      setShadowElevation(content, attributes.shadowElevation)
      setAmbientShadowColor(content, attributes.ambientShadowColor)
      setSpotShadowColor(content, attributes.spotShadowColor)
      setOutlineProvider(content, attributes.outlineProvider)
      setClipToOutline(content, attributes.clipToOutline)
      setClipChildren(content, attributes)
      setContentDescription(content, attributes.contentDescription)
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
      setViewStateListAnimator(content, attributes)
      if (attributes.disableDrawableOutputs) {
        setViewBackground(content, attributes)
        setViewForeground(content, attributes.foreground)

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
        setViewPadding(content, attributes)
        setViewForeground(content, attributes.foreground)
        setViewLayoutDirection(content, attributes)
      }

      if (content is ComponentHost) {
        content.setSafeViewModificationsEnabled(false)
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
      unsetClipToOutline(content, attributes.clipToOutline)
      unsetClipChildren(content, attributes.clipChildren)
      if (!attributes.contentDescription.isNullOrEmpty()) {
        unsetContentDescription(content)
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
        unsetViewPadding(content, attributes)
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

    private fun unsetAccessibilityDelegate(view: View) {
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

    private fun unsetClickHandler(view: View) {
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

    private fun unsetLongClickHandler(view: View) {
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

    private fun unsetFocusChangeHandler(view: View) {
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

    private fun unsetTouchHandler(view: View) {
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
        view.setInterceptTouchEventHandler(interceptTouchHandler)
      }
    }

    private fun unsetInterceptTouchEventHandler(view: View) {
      if (view is ComponentHost) {
        view.setInterceptTouchEventHandler(null)
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

    private fun unsetViewId(view: View) {
      view.id = View.NO_ID
    }

    private fun setViewTag(view: View, viewTag: Any?) {
      view.tag = viewTag
    }

    private fun setViewTags(view: View, viewTags: SparseArray<Any>?) {
      if (viewTags == null) {
        return
      }
      if (view is ComponentHost) {
        view.setViewTags(viewTags)
      } else {
        for (i in 0 until viewTags.size()) {
          view.setTag(viewTags.keyAt(i), viewTags.valueAt(i))
        }
      }
    }

    private fun unsetViewTag(view: View) {
      view.tag = null
    }

    private fun unsetViewTags(view: View, viewTags: SparseArray<Any>?) {
      if (view is ComponentHost) {
        view.setViewTags(null)
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
        view.outlineAmbientShadowColor = ambientShadowColor
      }
    }

    private fun setSpotShadowColor(view: View, @ColorInt spotShadowColor: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        view.outlineSpotShadowColor = spotShadowColor
      }
    }

    private fun unsetShadowElevation(view: View, shadowElevation: Float) {
      if (shadowElevation != 0f) {
        ViewCompat.setElevation(view, 0f)
      }
    }

    private fun unsetAmbientShadowColor(view: View, @ColorInt ambientShadowColor: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ambientShadowColor != Color.BLACK) {
        // Android documentation says black is the default:
        // https://developer.android.com/reference/android/view/View#getOutlineAmbientShadowColor()
        view.outlineAmbientShadowColor = Color.BLACK
      }
    }

    private fun unsetSpotShadowColor(view: View, @ColorInt spotShadowColor: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && spotShadowColor != Color.BLACK) {
        // Android documentation says black is the default:
        // https://developer.android.com/reference/android/view/View#getOutlineSpotShadowColor()
        view.outlineSpotShadowColor = Color.BLACK
      }
    }

    private fun setOutlineProvider(view: View, outlineProvider: ViewOutlineProvider?) {
      if (outlineProvider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        view.outlineProvider = outlineProvider
      }
    }

    private fun unsetOutlineProvider(view: View, outlineProvider: ViewOutlineProvider?) {
      if (outlineProvider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        view.outlineProvider = ViewOutlineProvider.BACKGROUND
      }
    }

    private fun setClipToOutline(view: View, clipToOutline: Boolean) {
      if (clipToOutline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        view.clipToOutline = clipToOutline
      }
    }

    private fun unsetClipToOutline(view: View, clipToOutline: Boolean) {
      if (clipToOutline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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

    private fun unsetContentDescription(view: View) {
      view.contentDescription = null
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

    private fun unsetFocusable(view: View, flags: Int) {
      view.isFocusable = LithoMountData.isViewFocusable(flags)
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

    private fun unsetTooltipText(view: View) {
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

    private fun setViewPadding(view: View, attributes: ViewAttributes) {
      if (!attributes.hasPadding()) {
        return
      }
      view.setPadding(
          attributes.paddingLeft,
          attributes.paddingTop,
          attributes.paddingRight,
          attributes.paddingBottom)
    }

    private fun unsetViewPadding(view: View, attributes: ViewAttributes) {
      if (!attributes.hasPadding()) {
        return
      }
      try {
        view.setPadding(0, 0, 0, 0)
      } catch (e: NullPointerException) {
        // T53931759 Gathering extra info around this NPE
        ErrorReporter.instance.report(
            LogLevel.ERROR,
            "LITHO:NPE:UNSET_PADDING",
            "From component: ${attributes.componentName}",
            e,
            0,
            null)
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
    private fun setBackgroundCompat(view: View, drawable: Drawable?) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        view.setBackgroundDrawable(drawable)
      } else {
        view.background = drawable
      }
    }

    private fun unsetViewForeground(view: View, attributes: ViewAttributes) {
      val foreground = attributes.foreground
      if (foreground != null) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          ("MountState has a ViewAttributes with foreground however the current Android version doesn't support foreground on Views")
        }
        view.foreground = null
      }
    }

    private fun setViewLayoutDirection(view: View, attributes: ViewAttributes) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        return
      }
      view.layoutDirection = attributes.layoutDirection.getLayoutDirectionForView()
    }

    private fun unsetViewLayoutDirection(view: View) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        return
      }
      view.layoutDirection = View.LAYOUT_DIRECTION_INHERIT
    }

    private fun setViewStateListAnimator(view: View, attributes: ViewAttributes) {
      var stateListAnimator = attributes.stateListAnimator
      val stateListAnimatorRes = attributes.stateListAnimatorRes
      if (stateListAnimator == null && stateListAnimatorRes == 0) {
        return
      }
      check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ("MountState has a ViewAttributes with stateListAnimator, however the current Android version doesn't support stateListAnimator on Views")
      }
      if (stateListAnimator == null) {
        stateListAnimator =
            AnimatorInflater.loadStateListAnimator(view.context, stateListAnimatorRes)
      }
      view.stateListAnimator = stateListAnimator
    }

    private fun unsetViewStateListAnimator(view: View, attributes: ViewAttributes) {
      if (attributes.stateListAnimator == null && attributes.stateListAnimatorRes == 0) {
        return
      }
      check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ("MountState has a ViewAttributes with stateListAnimator, however the current Android version doesn't support stateListAnimator on Views")
      }
      view.stateListAnimator.jumpToCurrentState()
      view.stateListAnimator = null
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

    @JvmStatic
    fun shouldUpdateViewInfo(
        nextAttributes: ViewAttributes?,
        currentAttributes: ViewAttributes?
    ): Boolean = !equals(currentAttributes, nextAttributes)
  }
}
