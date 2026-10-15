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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.utils.VersionedAndroidApis
import com.facebook.litho.view.ClickListenerHolder
import com.facebook.litho.visibility.Visibility
import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.utils.equals

class ViewAttributes {

  // ---- Properties with framework plumbing (not candidates for binder migration) ----

  var isHostSpec: Boolean = false
  var componentName: String = ""
  var disableDrawableOutputs: Boolean = false

  var visibility: Visibility?
    get() = binderMigratedAttributes?.visibility
    set(value) {
      getOrCreateBinderMigratedAttributes().visibility = value
    }

  internal var flags: Int = 0
    private set

  var isFocusable: Boolean
    get() = binderMigratedAttributes?.isFocusable ?: false
    set(value) {
      flags = flags or FLAG_FOCUS
      getOrCreateBinderMigratedAttributes().isFocusable = value
    }

  var isClickable: Boolean
    get() = binderMigratedAttributes?.isClickable ?: false
    set(value) {
      flags = flags or FLAG_CLICKABLE
      getOrCreateBinderMigratedAttributes().isClickable = value
    }

  var isEnabled: Boolean
    get() = binderMigratedAttributes?.isEnabled ?: false
    set(value) {
      flags = flags or FLAG_ENABLED
      getOrCreateBinderMigratedAttributes().isEnabled = value
    }

  var isSelected: Boolean
    get() = binderMigratedAttributes?.isSelected ?: false
    set(value) {
      flags = flags or FLAG_SELECTED
      getOrCreateBinderMigratedAttributes().isSelected = value
    }

  var isKeyboardNavigationCluster: Boolean
    get() = binderMigratedAttributes?.isKeyboardNavigationCluster ?: false
    set(value) {
      flags = flags or FLAG_KEYBOARD_NAVIGATION_CLUSTER
      getOrCreateBinderMigratedAttributes().isKeyboardNavigationCluster = value
    }

  @LayerType
  var layerType: Int
    get() = binderMigratedAttributes?.layerType ?: LayerType.LAYER_TYPE_NOT_SET
    set(value) {
      getOrCreateBinderMigratedAttributes().layerType = value
    }

  var layoutPaint: Paint?
    get() = binderMigratedAttributes?.layoutPaint
    set(value) {
      getOrCreateBinderMigratedAttributes().layoutPaint = value
    }

  val isFocusableSet: Boolean
    get() = binderMigratedAttributes?.isFocusableSet ?: false

  val isClickableSet: Boolean
    get() = binderMigratedAttributes?.isClickableSet ?: false

  val isEnabledSet: Boolean
    get() = binderMigratedAttributes?.isEnabledSet ?: false

  val isSelectedSet: Boolean
    get() = binderMigratedAttributes?.isSelectedSet ?: false

  val isKeyboardNavigationClusterSet: Boolean
    get() = binderMigratedAttributes?.isKeyboardNavigationClusterSet ?: false

  var contentDescription: CharSequence? = null
  var importantForAccessibility: Int = 0
  var accessibilityPaneTitle: CharSequence? = null
  var liveRegionMode: Int? = null
  var layoutDirection: LayoutDirection = LayoutDirection.INHERIT
  @DrawableRes var stateListAnimatorRes: Int = 0

  // ---- Layout-bounds dependent properties ----
  // These need resolved layout bounds at bind time, preventing straightforward binder migration.

  var systemGestureExclusionZones: List<(Rect) -> Rect>? = null

  // ---- Migrated properties: delegating to binderMigratedAttributes ----
  // These properties have been migrated to the onBindViewWithDescription binder pattern behind
  // the useViewStyleBinders config flag. When the binder migration is fully rolled out, these
  // delegating accessors (along with BinderMigratedViewAttributes) can be deleted.

  var binderMigratedAttributes: BinderMigratedViewAttributes? = null
    private set

  private fun getOrCreateBinderMigratedAttributes(): BinderMigratedViewAttributes {
    var attrs = binderMigratedAttributes
    if (attrs == null) {
      if (ComponentsConfiguration.defaultInstance.useViewStyleBinders) {
        DebugInfoReporter.report(
            category = "BinderMigratedViewAttributes",
            level = LogLevel.ERROR,
        ) {
          put("message", "Creating BinderMigratedViewAttributes while useViewStyleBinders is true")
        }
      }
      attrs = BinderMigratedViewAttributes()
      binderMigratedAttributes = attrs
    }
    return attrs
  }

  var clickHandler: EventHandler<ClickEvent>?
    get() = binderMigratedAttributes?.clickHandler
    set(value) {
      getOrCreateBinderMigratedAttributes().clickHandler = value
    }

  /**
   * Additive click listeners registered via `Style.clickListener(key, …)`. Composed with
   * [clickHandler] into a single [ComponentClickListener] (with the extras passed alongside the
   * primary [EventHandler]) and installed at mount.
   */
  internal var extraClickListeners: MutableList<ClickListenerHolder>?
    get() = binderMigratedAttributes?.extraClickListeners
    set(value) {
      getOrCreateBinderMigratedAttributes().extraClickListeners = value
    }

  var longClickHandler: EventHandler<LongClickEvent>?
    get() = binderMigratedAttributes?.longClickHandler
    set(value) {
      getOrCreateBinderMigratedAttributes().longClickHandler = value
    }

  var focusChangeHandler: EventHandler<FocusChangedEvent>?
    get() = binderMigratedAttributes?.focusChangeHandler
    set(value) {
      getOrCreateBinderMigratedAttributes().focusChangeHandler = value
    }

  var touchHandler: EventHandler<TouchEvent>?
    get() = binderMigratedAttributes?.touchHandler
    set(value) {
      getOrCreateBinderMigratedAttributes().touchHandler = value
    }

  var interceptTouchHandler: EventHandler<InterceptTouchEvent>?
    get() = binderMigratedAttributes?.interceptTouchHandler
    set(value) {
      getOrCreateBinderMigratedAttributes().interceptTouchHandler = value
    }

  var tooltipText: String?
    get() = binderMigratedAttributes?.tooltipText
    set(value) {
      getOrCreateBinderMigratedAttributes().tooltipText = value
    }

  var viewId: Int
    get() = binderMigratedAttributes?.viewId ?: View.NO_ID
    set(value) {
      getOrCreateBinderMigratedAttributes().viewId = value
    }

  var viewTag: Any?
    get() = binderMigratedAttributes?.viewTag
    set(value) {
      getOrCreateBinderMigratedAttributes().viewTag = value
    }

  var transitionName: String?
    get() = binderMigratedAttributes?.transitionName
    set(value) {
      getOrCreateBinderMigratedAttributes().transitionName = value
    }

  var outlineProvider: ViewOutlineProvider?
    get() = binderMigratedAttributes?.outlineProvider
    set(value) {
      getOrCreateBinderMigratedAttributes().outlineProvider = value
    }

  var renderEffect: LithoRenderEffect?
    get() = binderMigratedAttributes?.renderEffect
    set(value) {
      getOrCreateBinderMigratedAttributes().renderEffect = value
    }

  var background: Drawable?
    get() = binderMigratedAttributes?.background
    set(value) {
      getOrCreateBinderMigratedAttributes().background = value
    }

  var foreground: Drawable?
    get() = binderMigratedAttributes?.foreground
    set(value) {
      getOrCreateBinderMigratedAttributes().foreground = value
    }

  var stateListAnimator: StateListAnimator?
    get() = binderMigratedAttributes?.stateListAnimator
    set(value) {
      getOrCreateBinderMigratedAttributes().stateListAnimator = value
    }

  var scale: Float
    get() = binderMigratedAttributes?.scale ?: 1f
    set(value) {
      getOrCreateBinderMigratedAttributes().scale = value
    }

  var alpha: Float
    get() = binderMigratedAttributes?.alpha ?: 1f
    set(value) {
      getOrCreateBinderMigratedAttributes().alpha = value
    }

  var rotation: Float
    get() = binderMigratedAttributes?.rotation ?: 0f
    set(value) {
      getOrCreateBinderMigratedAttributes().rotation = value
    }

  var rotationX: Float
    get() = binderMigratedAttributes?.rotationX ?: 0f
    set(value) {
      getOrCreateBinderMigratedAttributes().rotationX = value
    }

  var rotationY: Float
    get() = binderMigratedAttributes?.rotationY ?: 0f
    set(value) {
      getOrCreateBinderMigratedAttributes().rotationY = value
    }

  var clipChildren: Boolean
    get() = binderMigratedAttributes?.clipChildren ?: true
    set(value) {
      getOrCreateBinderMigratedAttributes().clipChildren = value
    }

  var defaultFocusHighlightEnabled: Boolean
    get() = binderMigratedAttributes?.defaultFocusHighlightEnabled ?: true
    set(value) {
      getOrCreateBinderMigratedAttributes().defaultFocusHighlightEnabled = value
    }

  var clipToOutline: Boolean
    get() = binderMigratedAttributes?.clipToOutline ?: false
    set(value) {
      getOrCreateBinderMigratedAttributes().clipToOutline = value
    }

  var shadowElevation: Float
    get() = binderMigratedAttributes?.shadowElevation ?: 0f
    set(value) {
      getOrCreateBinderMigratedAttributes().shadowElevation = value
    }

  @get:ColorInt
  var ambientShadowColor: Int
    get() = binderMigratedAttributes?.ambientShadowColor ?: Color.BLACK
    set(value) {
      getOrCreateBinderMigratedAttributes().ambientShadowColor = value
    }

  @get:ColorInt
  var spotShadowColor: Int
    get() = binderMigratedAttributes?.spotShadowColor ?: Color.BLACK
    set(value) {
      getOrCreateBinderMigratedAttributes().spotShadowColor = value
    }

  var viewTags: SparseArray<Any>?
    get() = binderMigratedAttributes?.viewTags
    set(value) {
      getOrCreateBinderMigratedAttributes().viewTags = value
    }

  fun addViewTags(viewTags: SparseArray<Any>?) {
    getOrCreateBinderMigratedAttributes().addViewTags(viewTags)
  }

  val isClipChildrenSet: Boolean
    get() = binderMigratedAttributes?.isClipChildrenSet ?: false

  val isDefaultFocusHighlightEnabledSet: Boolean
    get() = binderMigratedAttributes?.isDefaultFocusHighlightEnabledSet ?: false

  val isScaleSet: Boolean
    get() = binderMigratedAttributes?.isScaleSet ?: false

  val isAlphaSet: Boolean
    get() = binderMigratedAttributes?.isAlphaSet ?: false

  val isRotationSet: Boolean
    get() = binderMigratedAttributes?.isRotationSet ?: false

  val isRotationXSet: Boolean
    get() = binderMigratedAttributes?.isRotationXSet ?: false

  val isRotationYSet: Boolean
    get() = binderMigratedAttributes?.isRotationYSet ?: false

  val isTagSet: Boolean
    get() = binderMigratedAttributes?.isTagSet ?: false

  val isViewIdSet: Boolean
    get() = binderMigratedAttributes?.isViewIdSet ?: false

  // ---- equals / hashCode / copyInto ----

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ViewAttributes) return false

    // Properties with framework plumbing
    if (isHostSpec != other.isHostSpec) return false
    if (componentName != other.componentName) return false
    if (disableDrawableOutputs != other.disableDrawableOutputs) return false
    if (flags != other.flags) return false
    if (!equals(contentDescription, other.contentDescription)) return false
    if (importantForAccessibility != other.importantForAccessibility) return false
    if (!equals(accessibilityPaneTitle, other.accessibilityPaneTitle)) return false
    if (!equals(liveRegionMode, other.liveRegionMode)) return false
    if (!equals(layoutDirection, other.layoutDirection)) return false
    if (stateListAnimatorRes != other.stateListAnimatorRes) return false

    // Migrated (delegated to binderMigratedAttributes)
    if (binderMigratedAttributes != other.binderMigratedAttributes) return false

    return true
  }

  override fun hashCode(): Int {
    // Properties with framework plumbing
    var result = isHostSpec.hashCode()
    result = 31 * result + componentName.hashCode()
    result = 31 * result + disableDrawableOutputs.hashCode()
    result = 31 * result + flags
    result = 31 * result + (contentDescription?.hashCode() ?: 0)
    result = 31 * result + importantForAccessibility
    result = 31 * result + (accessibilityPaneTitle?.hashCode() ?: 0)
    result = 31 * result + (liveRegionMode?.hashCode() ?: 0)
    result = 31 * result + layoutDirection.hashCode()
    result = 31 * result + stateListAnimatorRes

    // Migrated (delegated to binderMigratedAttributes)
    result = 31 * result + (binderMigratedAttributes?.hashCode() ?: 0)
    return result
  }

  fun copyInto(target: ViewAttributes) {
    // Properties with framework plumbing
    target.isHostSpec = isHostSpec
    target.componentName = componentName
    target.disableDrawableOutputs = disableDrawableOutputs
    contentDescription?.let { target.contentDescription = it }
    target.importantForAccessibility = importantForAccessibility
    accessibilityPaneTitle?.let { target.accessibilityPaneTitle = it }
    liveRegionMode?.let { target.liveRegionMode = it }
    target.layoutDirection = layoutDirection
    target.stateListAnimatorRes = stateListAnimatorRes

    // Migrated (delegated to binderMigratedAttributes)
    binderMigratedAttributes?.copyInto(target.getOrCreateBinderMigratedAttributes())
  }

  companion object {
    private const val FLAG_FOCUS = 1 shl 0
    private const val FLAG_CLICKABLE = 1 shl 1
    private const val FLAG_ENABLED = 1 shl 2
    private const val FLAG_SELECTED = 1 shl 3
    private const val FLAG_KEYBOARD_NAVIGATION_CLUSTER = 1 shl 4

    @JvmStatic
    fun setViewAttributes(
        content: Any?,
        attributes: ViewAttributes,
        unit: RenderUnit<*>?,
    ) {
      setViewAttributesWithNodeInfo(content, attributes, (unit as? LithoRenderUnit)?.nodeInfo)
    }

    internal fun setViewAttributesWithNodeInfo(
        content: Any?,
        attributes: ViewAttributes,
        nodeInfo: NodeInfo?,
    ) {
      if (content !is View) {
        return
      }

      if (content is ComponentHost) {
        content.setSafeViewModificationsEnabled(true)
      }

      attributes.visibility?.let { content.visibility = it.toViewVisibility() }

      setClickHandler(attributes.clickHandler, attributes.extraClickListeners, content)
      setLongClickHandler(attributes.longClickHandler, content)
      setFocusChangeHandler(attributes.focusChangeHandler, content)
      setTouchHandler(attributes.touchHandler, content)
      setInterceptTouchHandler(attributes.interceptTouchHandler, content)
      if (nodeInfo != null) setAccessibilityDelegate(content, nodeInfo)
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
      setDefaultFocusHighlightEnabled(content, attributes)
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
        setViewForeground(content, attributes.foreground)
        setViewLayoutDirection(content, attributes)
      }

      setSystemGestureExclusionZones(content, attributes.systemGestureExclusionZones)

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

      if (attributes.clickHandler != null || !attributes.extraClickListeners.isNullOrEmpty()) {
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
      unsetClipChildren(content, attributes, mountFlags)
      unsetDefaultFocusHighlightEnabled(content, attributes)
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
      unsetFocusable(content, attributes, mountFlags)
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

      unsetSystemGestureExclusionZones(content, attributes.systemGestureExclusionZones)
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
      if (view !is ComponentHost && AccessibilityUtils.isAccessibilityEnabled(view.context)) {
        ComponentHost.registerAccessibilityDelegateOnView(view, nodeInfo)
      }
    }

    fun unsetAccessibilityDelegate(view: View) {
      if (view !is ComponentHost && view.getTag(ComponentHost.COMPONENT_NODE_INFO_ID) == null) {
        return
      }
      view.setTag(ComponentHost.COMPONENT_NODE_INFO_ID, null)
      if (view !is ComponentHost) {
        ComponentHost.unregisterAccessibilityDelegateOnView(view)
      }
    }

    /**
     * Installs the click listener for this view. The listener is composed at render time (when
     * [ViewAttributes] are assembled) from the optional primary [clickHandler] and any extra
     * listeners registered via `Style.clickListener(key, …)`. At mount this method just installs
     * the pre-composed listener atomically — there is no per-binder mutation of a shared composite.
     *
     * - Primary (with or without extras) → [ComponentClickListener]. Extras dispatch inside the
     *   same `ComponentsSystrace` + touch-event debugger envelope as the primary.
     * - Extras only (no primary) → an inline [View.OnClickListener] that builds a [ClickEvent] and
     *   dispatches each extra in order.
     */
    private fun setClickHandler(
        clickHandler: EventHandler<ClickEvent>?,
        extra: List<ClickListenerHolder>?,
        view: View,
    ) {
      val hasExtra = !extra.isNullOrEmpty()
      if (clickHandler == null && !hasExtra) {
        return
      }
      val listener: View.OnClickListener =
          if (clickHandler != null) {
            ComponentClickListener(
                clickHandler,
                if (hasExtra) extra!!.map { it.onClick } else emptyList(),
            )
          } else {
            // Extras-only path — no primary to trace through. A plain [View.OnClickListener]
            // builds the [ClickEvent] and fans out to each extra in insertion order.
            val extraLambdas = extra!!.map { it.onClick }
            View.OnClickListener { v ->
              val event = ClickEvent().apply { this.view = v }
              for (i in extraLambdas.indices) {
                extraLambdas[i](event)
              }
            }
          }
      view.setOnClickListener(listener)
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
        view: View,
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
        view: View,
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

    private fun unsetClipChildren(view: View, attributes: ViewAttributes, flags: Int) {
      if (attributes.isClipChildrenSet && view is ViewGroup) {
        // There is no single default to restore: a ComponentHost is created not clipping, while a
        // plain ViewGroup follows the platform default of clipping. Restore whatever the content
        // itself had before this component wrote to it.
        view.clipChildren = LithoMountData.isViewClipChildren(flags)
      }
    }

    private fun setDefaultFocusHighlightEnabled(view: View, attributes: ViewAttributes) {
      if (
          attributes.isDefaultFocusHighlightEnabledSet &&
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
      ) {
        VersionedAndroidApis.O.setDefaultFocusHighlightEnabled(
            view,
            attributes.defaultFocusHighlightEnabled,
        )
      }
    }

    private fun unsetDefaultFocusHighlightEnabled(view: View, attributes: ViewAttributes) {
      // Platform default for defaultFocusHighlightEnabled is 'true'. If it was set to a non-default
      // value before mounting, reset it here on recycling.
      if (
          attributes.isDefaultFocusHighlightEnabledSet &&
              !attributes.defaultFocusHighlightEnabled &&
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
      ) {
        VersionedAndroidApis.O.setDefaultFocusHighlightEnabled(view, true)
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

    fun unsetFocusable(view: View, attributes: ViewAttributes, flags: Int) {
      if (attributes.isFocusableSet) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          view.focusable = LithoMountData.getViewFocusable(flags)
        } else {
          view.isFocusable = LithoMountData.isViewFocusable(flags)
        }
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
          view,
          LithoMountData.isViewKeyboardNavigationCluster(flags),
      )
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

    private fun setViewForeground(view: View, foreground: Drawable?) {
      if (foreground != null) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          ("MountState has a ViewAttributes with foreground however the current Android version doesn't support foreground on Views")
        }
        //noinspection AndroidLintNewApi
        view.foreground = foreground
      }
    }

    private fun unsetViewForeground(view: View, attributes: ViewAttributes) {
      val foreground = attributes.foreground
      if (foreground != null) {
        unsetForeground(view)
      }
    }

    fun unsetForeground(view: View) {
      check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ("MountState has a ViewAttributes with foreground however the current Android version doesn't support foreground on Views")
      }
      //noinspection AndroidLintNewApi
      view.foreground = null
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
    ) {
      val stateListAnimator = attributes.stateListAnimator
      val stateListAnimatorRes = attributes.stateListAnimatorRes
      if (stateListAnimator == null && stateListAnimatorRes == 0) {
        return
      }
      view.stateListAnimator =
          stateListAnimator
              ?: AnimatorInflater.loadStateListAnimator(view.context, stateListAnimatorRes)
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
        // Guard against the uncatchable hwui "Unable to create layer ... max size N" native abort
        // when a HARDWARE layer exceeds the device's GL_MAX_TEXTURE_SIZE (see
        // LayerType.guardHardwareLayerForSize). No-op unless LayerType.maxHardwareLayerSizePx is
        // set.
        view.setLayerType(
            LayerTypeGuard.guardHardwareLayerForSize(view, type),
            attributes.layoutPaint,
        )
      }
    }

    private fun unsetViewLayerType(view: View, mountFlags: Int) {
      val type = LithoMountData.getOriginalLayerType(mountFlags)
      if (type != LayerType.LAYER_TYPE_NOT_SET) {
        view.setLayerType(type, null)
      }
    }

    private fun setSystemGestureExclusionZones(
        view: View,
        zones: List<(Rect) -> Rect>?,
    ) {
      if (zones.isNullOrEmpty()) {
        return
      }
      val bounds = Rect(0, 0, view.width, view.height)
      val exclusionRects = zones.map { it(bounds) }
      ViewCompat.setSystemGestureExclusionRects(view, exclusionRects)
    }

    private fun unsetSystemGestureExclusionZones(
        view: View,
        zones: List<(Rect) -> Rect>?,
    ) {
      if (zones.isNullOrEmpty()) {
        return
      }
      ViewCompat.setSystemGestureExclusionRects(view, emptyList())
    }
  }
}
