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
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.ViewOutlineProvider
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.util.Preconditions
import com.facebook.infer.annotation.ThreadConfined
import com.facebook.litho.AccessibilityRole.AccessibilityRoleType
import com.facebook.litho.Transition.TransitionKeyType
import com.facebook.litho.drawable.DrawableUtils
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.visibility.Visibility
import com.facebook.rendercore.Equivalence
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.utils.equals
import com.facebook.rendercore.utils.isEquivalentTo
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaPositionType

/** Internal class that holds props that are common to all [Component]s. */
@ThreadConfined(ThreadConfined.ANY)
class CommonProps : LayoutProps, Equivalence<CommonProps?> {

  private var privateFlags: Int = 0

  @AttrRes
  var defStyleAttr: Int = 0
    private set

  @StyleRes
  var defStyleRes: Int = 0
    private set

  private var _nodeInfo: NodeInfo? = null
  val nodeInfo: NodeInfo?
    get() = _nodeInfo

  private fun getOrCreateNodeInfo(): NodeInfo {
    return _nodeInfo ?: NodeInfo().apply { _nodeInfo = this }
  }

  private var _otherProps: OtherProps? = null
  private val otherProps: OtherProps
    get() {
      return _otherProps ?: OtherProps().also { _otherProps = it }
    }

  private var _layoutProps: DefaultLayoutProps? = null
  private val layoutProps: LayoutProps
    get() {
      return _layoutProps ?: DefaultLayoutProps().also { _layoutProps = it }
    }

  private var _commonDynamicProps: SparseArray<DynamicValue<*>>? = null
  val commonDynamicProps: SparseArray<DynamicValue<*>>?
    get() {
      return _commonDynamicProps
    }

  fun getOrCreateDynamicProps(): SparseArray<DynamicValue<*>> {
    return _commonDynamicProps ?: SparseArray<DynamicValue<*>>().also { _commonDynamicProps = it }
  }

  fun hasCommonDynamicProps(): Boolean {
    return CollectionsUtils.isNotNullOrEmpty(_commonDynamicProps)
  }

  private var wrapInView: Boolean = false

  var background: Drawable? = null
    private set

  var paddingFromBackground: Rect? = null
    private set

  private var _testKey: String? = null

  var componentTag: Any? = null
    private set

  fun setStyle(@AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int) {
    this.defStyleAttr = defStyleAttr
    this.defStyleRes = defStyleRes
  }

  override fun positionType(positionType: YogaPositionType) {
    layoutProps.positionType(positionType)
  }

  override fun positionPx(edge: YogaEdge, @Px position: Int) {
    layoutProps.positionPx(edge, position)
  }

  override fun widthPx(@Px width: Int) {
    layoutProps.widthPx(width)
  }

  override fun heightPx(@Px height: Int) {
    layoutProps.heightPx(height)
  }

  fun background(background: Drawable?) {
    privateFlags = (privateFlags or PFLAG_BACKGROUND_IS_SET)
    this.background = background
    if (background != null) {
      val outRect = Rect()
      background.getPadding(outRect)
      if (outRect.bottom != 0 || outRect.top != 0 || outRect.left != 0 || outRect.right != 0) {
        paddingFromBackground = outRect
      }
    }
  }

  fun testKey(testKey: String?) {
    privateFlags = (privateFlags or PFLAG_TEST_KEY_IS_SET)
    _testKey = testKey
  }

  val testKey: String?
    get() = if (privateFlags and PFLAG_TEST_KEY_IS_SET != 0) _testKey else null

  fun componentTag(componentTag: Any?) {
    this.componentTag = componentTag
  }

  fun wrapInView() {
    wrapInView = true
  }

  private fun shouldWrapInView(): Boolean {
    return (wrapInView ||
        (privateFlags and
            (PFLAG_SCALE_KEY_IS_SET or PFLAG_ALPHA_KEY_IS_SET or PFLAG_ROTATION_KEY_IS_SET)) != 0)
  }

  override fun layoutDirection(direction: LayoutDirection) {
    layoutProps.layoutDirection(direction)
  }

  override fun alignSelf(alignSelf: YogaAlign) {
    layoutProps.alignSelf(alignSelf)
  }

  override fun flex(flex: Float) {
    layoutProps.flex(flex)
  }

  override fun flexGrow(flexGrow: Float) {
    layoutProps.flexGrow(flexGrow)
  }

  override fun flexShrink(flexShrink: Float) {
    layoutProps.flexShrink(flexShrink)
  }

  override fun flexBasisPx(@Px flexBasis: Int) {
    layoutProps.flexBasisPx(flexBasis)
  }

  override fun flexBasisPercent(percent: Float) {
    layoutProps.flexBasisPercent(percent)
  }

  fun importantForAccessibility(importantForAccessibility: Int) {
    otherProps.importantForAccessibility(importantForAccessibility)
  }

  fun duplicateParentState(duplicateParentState: Boolean) {
    otherProps.duplicateParentState(duplicateParentState)
  }

  fun duplicateChildrenStates(duplicateChildrenStates: Boolean) {
    otherProps.duplicateChildrenStates(duplicateChildrenStates)
  }

  override fun marginPx(edge: YogaEdge, @Px margin: Int) {
    layoutProps.marginPx(edge, margin)
  }

  override fun marginPercent(edge: YogaEdge, percent: Float) {
    layoutProps.marginPercent(edge, percent)
  }

  override fun marginAuto(edge: YogaEdge) {
    layoutProps.marginAuto(edge)
  }

  override fun paddingPx(edge: YogaEdge, @Px padding: Int) {
    layoutProps.paddingPx(edge, padding)
  }

  override fun paddingPercent(edge: YogaEdge, percent: Float) {
    layoutProps.paddingPercent(edge, percent)
  }

  fun border(border: Border?) {
    otherProps.border(border)
  }

  fun stateListAnimator(stateListAnimator: StateListAnimator?) {
    otherProps.stateListAnimator(stateListAnimator)
  }

  fun stateListAnimatorRes(@DrawableRes resId: Int) {
    otherProps.stateListAnimatorRes(resId)
  }

  override fun positionPercent(edge: YogaEdge, percent: Float) {
    layoutProps.positionPercent(edge, percent)
  }

  override fun widthPercent(percent: Float) {
    layoutProps.widthPercent(percent)
  }

  override fun minWidthPx(@Px minWidth: Int) {
    layoutProps.minWidthPx(minWidth)
  }

  override fun minWidthPercent(percent: Float) {
    layoutProps.minWidthPercent(percent)
  }

  override fun maxWidthPx(@Px maxWidth: Int) {
    layoutProps.maxWidthPx(maxWidth)
  }

  override fun maxWidthPercent(percent: Float) {
    layoutProps.maxWidthPercent(percent)
  }

  override fun heightPercent(percent: Float) {
    layoutProps.heightPercent(percent)
  }

  override fun minHeightPx(@Px minHeight: Int) {
    layoutProps.minHeightPx(minHeight)
  }

  override fun minHeightPercent(percent: Float) {
    layoutProps.minHeightPercent(percent)
  }

  override fun maxHeightPx(@Px maxHeight: Int) {
    layoutProps.maxHeightPx(maxHeight)
  }

  override fun maxHeightPercent(percent: Float) {
    layoutProps.maxHeightPercent(percent)
  }

  override fun aspectRatio(aspectRatio: Float) {
    layoutProps.aspectRatio(aspectRatio)
  }

  override fun isReferenceBaseline(isReferenceBaseline: Boolean) {
    layoutProps.isReferenceBaseline(isReferenceBaseline)
  }

  override fun useHeightAsBaseline(useHeightAsBaseline: Boolean) {
    layoutProps.useHeightAsBaseline(useHeightAsBaseline)
  }

  /** Used by [DebugLayoutNodeEditor] */
  override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) {
    layoutProps.setBorderWidth(edge, borderWidth)
  }

  override fun gap(gutter: YogaGutter, length: Int) {
    layoutProps.gap(gutter, length)
  }

  fun touchExpansionPx(edge: YogaEdge, @Px touchExpansion: Int) {
    otherProps.touchExpansionPx(edge, touchExpansion)
  }

  fun foreground(foreground: Drawable?) {
    otherProps.foreground(foreground)
  }

  fun clickHandler(clickHandler: EventHandler<ClickEvent>?) {
    getOrCreateNodeInfo().clickHandler = clickHandler
  }

  val clickHandler: EventHandler<ClickEvent>?
    get() = _nodeInfo?.clickHandler

  fun longClickHandler(longClickHandler: EventHandler<LongClickEvent>?) {
    getOrCreateNodeInfo().longClickHandler = longClickHandler
  }

  val longClickHandler: EventHandler<LongClickEvent>?
    get() = _nodeInfo?.longClickHandler

  fun focusChangeHandler(focusChangeHandler: EventHandler<FocusChangedEvent>?) {
    getOrCreateNodeInfo().focusChangeHandler = focusChangeHandler
  }

  val focusChangeHandler: EventHandler<FocusChangedEvent>?
    get() = _nodeInfo?.focusChangeHandler

  fun touchHandler(touchHandler: EventHandler<TouchEvent>?) {
    getOrCreateNodeInfo().touchHandler = touchHandler
  }

  val touchHandler: EventHandler<TouchEvent>?
    get() = _nodeInfo?.touchHandler

  fun interceptTouchHandler(interceptTouchHandler: EventHandler<InterceptTouchEvent>?) {
    getOrCreateNodeInfo().interceptTouchHandler = interceptTouchHandler
  }

  val interceptTouchHandler: EventHandler<InterceptTouchEvent>?
    get() = _nodeInfo?.interceptTouchHandler

  fun focusable(isFocusable: Boolean) {
    getOrCreateNodeInfo().setFocusable(isFocusable)
  }

  val focusable: Boolean
    get() = _nodeInfo?.focusState == NodeInfo.FOCUS_SET_TRUE

  fun clickable(isClickable: Boolean) {
    getOrCreateNodeInfo().setClickable(isClickable)
  }

  fun enabled(isEnabled: Boolean) {
    getOrCreateNodeInfo().setEnabled(isEnabled)
  }

  val isEnabled: Boolean
    get() = _nodeInfo?.enabledState == NodeInfo.ENABLED_SET_TRUE

  fun selected(isSelected: Boolean) {
    getOrCreateNodeInfo().setSelected(isSelected)
  }

  fun accessibilityHeading(isHeading: Boolean) {
    getOrCreateNodeInfo().setAccessibilityHeading(isHeading)
  }

  fun visibleHeightRatio(visibleHeightRatio: Float) {
    otherProps.visibleHeightRatio(visibleHeightRatio)
  }

  fun visibleWidthRatio(visibleWidthRatio: Float) {
    otherProps.visibleWidthRatio(visibleWidthRatio)
  }

  fun visibleHandler(visibleHandler: EventHandler<VisibleEvent>?) {
    otherProps.visibleHandler(visibleHandler)
  }

  fun focusedHandler(focusedHandler: EventHandler<FocusedVisibleEvent>?) {
    otherProps.focusedHandler(focusedHandler)
  }

  fun unfocusedHandler(unfocusedHandler: EventHandler<UnfocusedVisibleEvent>?) {
    otherProps.unfocusedHandler(unfocusedHandler)
  }

  fun fullImpressionHandler(fullImpressionHandler: EventHandler<FullImpressionVisibleEvent>?) {
    otherProps.fullImpressionHandler(fullImpressionHandler)
  }

  fun invisibleHandler(invisibleHandler: EventHandler<InvisibleEvent>?) {
    otherProps.invisibleHandler(invisibleHandler)
  }

  fun visibilityChangedHandler(visibilityChangedHandler: EventHandler<VisibilityChangedEvent>?) {
    otherProps.visibilityChangedHandler(visibilityChangedHandler)
  }

  fun setVisibility(visibility: Visibility) {
    getOrCreateNodeInfo().visibility = visibility
    wrapInView()
  }

  val visibility: Visibility?
    get() = _nodeInfo?.visibility

  fun contentDescription(contentDescription: CharSequence?) {
    getOrCreateNodeInfo().contentDescription = contentDescription
  }

  val contentDescription: CharSequence?
    get() = _nodeInfo?.contentDescription

  fun viewTag(viewTag: Any?) {
    getOrCreateNodeInfo().viewTag = viewTag
  }

  fun viewId(@IdRes id: Int) {
    getOrCreateNodeInfo().viewId = id
  }

  fun viewTags(viewTags: SparseArray<Any>?) {
    getOrCreateNodeInfo().viewTags = viewTags
  }

  fun transitionName(transitionName: String?) {
    getOrCreateNodeInfo().transitionName = transitionName
  }

  fun shadowElevationPx(shadowElevation: Float) {
    getOrCreateNodeInfo().shadowElevation = shadowElevation
  }

  fun ambientShadowColor(@ColorInt ambientShadowColor: Int) {
    getOrCreateNodeInfo().ambientShadowColor = ambientShadowColor
  }

  fun spotShadowColor(@ColorInt spotShadowColor: Int) {
    getOrCreateNodeInfo().spotShadowColor = spotShadowColor
  }

  fun outlineProvider(outlineProvider: ViewOutlineProvider?) {
    getOrCreateNodeInfo().outlineProvider = outlineProvider
  }

  fun clipToOutline(clipToOutline: Boolean) {
    getOrCreateNodeInfo().clipToOutline = clipToOutline
  }

  fun clipChildren(clipChildren: Boolean) {
    getOrCreateNodeInfo().clipChildren = clipChildren
  }

  fun accessibilityRole(@AccessibilityRoleType role: String?) {
    getOrCreateNodeInfo().accessibilityRole = role
  }

  fun accessibilityRoleDescription(roleDescription: CharSequence?) {
    getOrCreateNodeInfo().accessibilityRoleDescription = roleDescription
  }

  fun dispatchPopulateAccessibilityEventHandler(
      dispatchPopulateAccessibilityEventHandler:
          EventHandler<DispatchPopulateAccessibilityEventEvent>?
  ) {
    getOrCreateNodeInfo().dispatchPopulateAccessibilityEventHandler =
        dispatchPopulateAccessibilityEventHandler
  }

  fun onInitializeAccessibilityEventHandler(
      onInitializeAccessibilityEventHandler: EventHandler<OnInitializeAccessibilityEventEvent>?
  ) {
    getOrCreateNodeInfo().onInitializeAccessibilityEventHandler =
        onInitializeAccessibilityEventHandler
  }

  fun onInitializeAccessibilityNodeInfoHandler(
      onInitializeAccessibilityNodeInfoHandler:
          EventHandler<OnInitializeAccessibilityNodeInfoEvent>?
  ) {
    getOrCreateNodeInfo().onInitializeAccessibilityNodeInfoHandler =
        onInitializeAccessibilityNodeInfoHandler
  }

  fun onPopulateAccessibilityEventHandler(
      onPopulateAccessibilityEventHandler: EventHandler<OnPopulateAccessibilityEventEvent>?
  ) {
    getOrCreateNodeInfo().onPopulateAccessibilityEventHandler = onPopulateAccessibilityEventHandler
  }

  fun onPopulateAccessibilityNodeHandler(
      onPopulateAccessibilityNodeHandler: EventHandler<OnPopulateAccessibilityNodeEvent>?
  ) {
    getOrCreateNodeInfo().onPopulateAccessibilityNodeHandler = onPopulateAccessibilityNodeHandler
  }

  fun onRequestSendAccessibilityEventHandler(
      onRequestSendAccessibilityEventHandler: EventHandler<OnRequestSendAccessibilityEventEvent>?
  ) {
    getOrCreateNodeInfo().onRequestSendAccessibilityEventHandler =
        onRequestSendAccessibilityEventHandler
  }

  fun performAccessibilityActionHandler(
      performAccessibilityActionHandler: EventHandler<PerformAccessibilityActionEvent>?
  ) {
    getOrCreateNodeInfo().performAccessibilityActionHandler = performAccessibilityActionHandler
  }

  fun sendAccessibilityEventHandler(
      sendAccessibilityEventHandler: EventHandler<SendAccessibilityEventEvent>?
  ) {
    getOrCreateNodeInfo().sendAccessibilityEventHandler = sendAccessibilityEventHandler
  }

  fun sendAccessibilityEventUncheckedHandler(
      sendAccessibilityEventUncheckedHandler: EventHandler<SendAccessibilityEventUncheckedEvent>?
  ) {
    getOrCreateNodeInfo().sendAccessibilityEventUncheckedHandler =
        sendAccessibilityEventUncheckedHandler
  }

  fun onVirtualViewKeyboardFocusChangedHandler(
      onVirtualViewKeyboardFocusChangedHandler: EventHandler<VirtualViewKeyboardFocusChangedEvent>?
  ) {
    getOrCreateNodeInfo().onVirtualViewKeyboardFocusChangedHandler =
        onVirtualViewKeyboardFocusChangedHandler
  }

  fun onPerformActionForVirtualViewHandler(
      onPerformActionForVirtualViewHandler: EventHandler<PerformActionForVirtualViewEvent>?
  ) {
    getOrCreateNodeInfo().onPerformActionForVirtualViewHandler =
        onPerformActionForVirtualViewHandler
  }

  fun keyboardNavigationCluster(isKeyboardNavigationCluster: Boolean) {
    getOrCreateNodeInfo().setKeyboardNavigationCluster(isKeyboardNavigationCluster)
  }

  fun tooltipText(tooltipText: String?) {
    getOrCreateNodeInfo().tooltipText = tooltipText
  }

  val isKeyboardNavigationCluster: Boolean
    get() =
        (_nodeInfo?.keyboardNavigationClusterState == NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_TRUE)

  fun scale(scale: Float) {
    getOrCreateNodeInfo().scale = scale
    privateFlags =
        if (scale == 1f) {
          (privateFlags and PFLAG_SCALE_KEY_IS_SET.inv())
        } else {
          (privateFlags or PFLAG_SCALE_KEY_IS_SET)
        }
  }

  fun alpha(alpha: Float) {
    getOrCreateNodeInfo().alpha = alpha
    privateFlags =
        if (alpha == 1f) {
          (privateFlags and PFLAG_ALPHA_KEY_IS_SET.inv())
        } else {
          (privateFlags or PFLAG_ALPHA_KEY_IS_SET)
        }
  }

  fun rotation(rotation: Float) {
    getOrCreateNodeInfo().rotation = rotation
    privateFlags =
        if (rotation == 0f) {
          (privateFlags and PFLAG_ROTATION_KEY_IS_SET.inv())
        } else {
          (privateFlags or PFLAG_ROTATION_KEY_IS_SET)
        }
  }

  fun rotationX(rotationX: Float) {
    wrapInView()
    getOrCreateNodeInfo().rotationX = rotationX
  }

  fun rotationY(rotationY: Float) {
    wrapInView()
    getOrCreateNodeInfo().rotationY = rotationY
  }

  fun transitionKey(key: String?, ownerKey: String?) {
    otherProps.transitionKey(key, ownerKey)
  }

  fun getTransitionKey(): String? {
    return _otherProps?.getTransitionKey()
  }

  fun delegateMountViewBinder(binder: DelegateBinder<Any, Any, Any>) {
    otherProps.delegateMountViewBinder(binder)
  }

  val delegateViewBinders: Map<Class<*>, DelegateBinder<Any, Any, Any>>?
    get() = _otherProps?.typeToDelegateViewBinder

  fun transitionKeyType(type: TransitionKeyType?) {
    otherProps.transitionKeyType(type)
  }

  fun getTransitionKeyType(): TransitionKeyType? {
    return _otherProps?.getTransitionKeyType()
  }

  fun layerType(@LayerType type: Int, paint: Paint?) {
    otherProps.layerType(type, paint)
  }

  fun visibilityOutputTag(visibilityOutputTag: String?) {
    otherProps.visibilityOutputTag(visibilityOutputTag)
  }

  fun copyLayoutProps(layoutProps: LayoutProps) {
    _layoutProps?.copyInto(layoutProps)
  }

  fun addSystemGestureExclusionZone(exclusion: (Rect) -> Rect) {
    otherProps.addSystemGestureExclusionZone(exclusion)
  }

  fun copyInto(c: ComponentContext, node: LithoNode) {
    c.applyStyle(node, defStyleAttr, defStyleRes)
    if (_nodeInfo != null) {
      node.applyNodeInfo(_nodeInfo)
    }
    if ((privateFlags and PFLAG_BACKGROUND_IS_SET) != 0) {
      node.background(background)
      node.paddingFromBackground = paddingFromBackground
    }

    _layoutProps?.layoutDirection?.let { node.layoutDirection(it) }

    if ((privateFlags and PFLAG_TEST_KEY_IS_SET) != 0) {
      node.testKey(_testKey)
    }
    if (shouldWrapInView()) {
      node.wrapInView()
    }
    _otherProps?.copyInto(node)
  }

  override fun isEquivalentTo(other: CommonProps?): Boolean {
    if (this === other) {
      return true
    }
    return if (other == null) {
      false
    } else
        (privateFlags == other.privateFlags &&
            wrapInView == other.wrapInView &&
            defStyleAttr == other.defStyleAttr &&
            defStyleRes == other.defStyleRes &&
            DrawableUtils.isEquivalentTo(background, other.background) &&
            isEquivalentTo(_otherProps, other._otherProps) &&
            isEquivalentTo(_nodeInfo, other._nodeInfo) &&
            isEquivalentTo(_layoutProps, other._layoutProps) &&
            equals(_testKey, other._testKey) &&
            equals(componentTag, other.componentTag) &&
            equals(_commonDynamicProps, other._commonDynamicProps))
  }

  private class OtherProps : Equivalence<OtherProps?> {
    private var privateFlags: Int = 0
    private var visibleHeightRatio: Float = 0f
    private var visibleWidthRatio: Float = 0f
    private var visibleHandler: EventHandler<VisibleEvent>? = null
    private var focusedHandler: EventHandler<FocusedVisibleEvent>? = null
    private var unfocusedHandler: EventHandler<UnfocusedVisibleEvent>? = null
    private var fullImpressionHandler: EventHandler<FullImpressionVisibleEvent>? = null
    private var invisibleHandler: EventHandler<InvisibleEvent>? = null
    private var visibilityChangedHandler: EventHandler<VisibilityChangedEvent>? = null
    var typeToDelegateViewBinder: MutableMap<Class<*>, DelegateBinder<Any, Any, Any>>? = null
    private var importantForAccessibility: Int = 0
    private var duplicateParentState: Boolean = false
    private var duplicateChildrenStates: Boolean = false
    private var touchExpansions: Edges? = null
    private var foreground: Drawable? = null
    private var transitionOwnerKey: String? = null
    private var transitionKey: String? = null
    private var transitionKeyType: TransitionKeyType? = null
    private var border: Border? = null
    private var stateListAnimator: StateListAnimator? = null

    @DrawableRes private var stateListAnimatorRes: Int = 0
    private var layerType = LayerType.LAYER_TYPE_NOT_SET
    private var layerPaint: Paint? = null
    private var visibilityOutputTag: String? = null

    private var systemGestureExclusionZones: MutableList<(Rect) -> Rect>? = null

    fun delegateMountViewBinder(binder: DelegateBinder<Any, Any, Any>) {
      typeToDelegateViewBinder =
          (typeToDelegateViewBinder ?: LinkedHashMap()).apply {
            this[binder.delegatedBinderClass] = binder
          }
    }

    fun importantForAccessibility(importantForAccessibility: Int) {
      privateFlags = privateFlags or PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET
      this.importantForAccessibility = importantForAccessibility
    }

    fun duplicateParentState(duplicateParentState: Boolean) {
      privateFlags = privateFlags or PFLAG_DUPLICATE_PARENT_STATE_IS_SET
      this.duplicateParentState = duplicateParentState
    }

    fun duplicateChildrenStates(duplicateChildrenStates: Boolean) {
      privateFlags = privateFlags or PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET
      this.duplicateChildrenStates = duplicateChildrenStates
    }

    fun border(border: Border?) {
      if (border != null) {
        privateFlags = privateFlags or PFLAG_BORDER_IS_SET
        this.border = border
      }
    }

    fun touchExpansionPx(edge: YogaEdge, @Px touchExpansion: Int) {
      privateFlags = privateFlags or PFLAG_TOUCH_EXPANSION_IS_SET
      touchExpansions = (touchExpansions ?: Edges()).apply { this[edge] = touchExpansion.toFloat() }
    }

    fun foreground(foreground: Drawable?) {
      privateFlags = privateFlags or PFLAG_FOREGROUND_IS_SET
      this.foreground = foreground
    }

    fun visibleHeightRatio(visibleHeightRatio: Float) {
      privateFlags = privateFlags or PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET
      this.visibleHeightRatio = visibleHeightRatio
    }

    fun visibleWidthRatio(visibleWidthRatio: Float) {
      privateFlags = privateFlags or PFLAG_VISIBLE_WIDTH_RATIO_IS_SET
      this.visibleWidthRatio = visibleWidthRatio
    }

    fun visibleHandler(visibleHandler: EventHandler<VisibleEvent>?) {
      privateFlags = privateFlags or PFLAG_VISIBLE_HANDLER_IS_SET
      this.visibleHandler = visibleHandler
    }

    fun focusedHandler(focusedHandler: EventHandler<FocusedVisibleEvent>?) {
      privateFlags = privateFlags or PFLAG_FOCUSED_HANDLER_IS_SET
      this.focusedHandler = focusedHandler
    }

    fun unfocusedHandler(unfocusedHandler: EventHandler<UnfocusedVisibleEvent>?) {
      privateFlags = privateFlags or PFLAG_UNFOCUSED_HANDLER_IS_SET
      this.unfocusedHandler = unfocusedHandler
    }

    fun fullImpressionHandler(fullImpressionHandler: EventHandler<FullImpressionVisibleEvent>?) {
      privateFlags = privateFlags or PFLAG_FULL_IMPRESSION_HANDLER_IS_SET
      this.fullImpressionHandler = fullImpressionHandler
    }

    fun invisibleHandler(invisibleHandler: EventHandler<InvisibleEvent>?) {
      privateFlags = privateFlags or PFLAG_INVISIBLE_HANDLER_IS_SET
      this.invisibleHandler = invisibleHandler
    }

    fun visibilityChangedHandler(visibilityChangedHandler: EventHandler<VisibilityChangedEvent>?) {
      privateFlags = privateFlags or PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET
      this.visibilityChangedHandler = visibilityChangedHandler
    }

    fun transitionKey(key: String?, ownerKey: String?) {
      privateFlags = privateFlags or PFLAG_TRANSITION_KEY_IS_SET
      transitionKey = key
      transitionOwnerKey = ownerKey
    }

    fun getTransitionKey(): String? {
      return transitionKey
    }

    fun transitionKeyType(type: TransitionKeyType?) {
      privateFlags = privateFlags or PFLAG_TRANSITION_KEY_TYPE_IS_SET
      transitionKeyType = type
    }

    fun getTransitionKeyType(): TransitionKeyType? {
      return transitionKeyType
    }

    fun stateListAnimator(stateListAnimator: StateListAnimator?) {
      privateFlags = privateFlags or PFLAG_STATE_LIST_ANIMATOR_IS_SET
      this.stateListAnimator = stateListAnimator
    }

    fun stateListAnimatorRes(@DrawableRes resId: Int) {
      privateFlags = privateFlags or PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET
      stateListAnimatorRes = resId
    }

    fun layerType(@LayerType type: Int, paint: Paint?) {
      layerType = type
      layerPaint = paint
    }

    fun visibilityOutputTag(visibilityOutputTag: String?) {
      this.visibilityOutputTag = visibilityOutputTag
    }

    fun addSystemGestureExclusionZone(exclusion: (Rect) -> Rect) {
      (systemGestureExclusionZones
              ?: ArrayList<(Rect) -> Rect>().also { systemGestureExclusionZones = it })
          .add(exclusion)
    }

    fun copyInto(node: LithoNode) {
      if ((privateFlags and PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET).toLong() != 0L) {
        node.importantForAccessibility(importantForAccessibility)
      }
      if ((privateFlags and PFLAG_DUPLICATE_PARENT_STATE_IS_SET).toLong() != 0L) {
        node.duplicateParentState(duplicateParentState)
      }
      if ((privateFlags and PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET).toLong() != 0L) {
        node.duplicateChildrenStates(duplicateChildrenStates)
      }
      if ((privateFlags and PFLAG_FOREGROUND_IS_SET).toLong() != 0L) {
        node.foreground(foreground)
      }
      if ((privateFlags and PFLAG_WRAP_IN_VIEW_IS_SET).toLong() != 0L) {
        node.wrapInView()
      }
      if ((privateFlags and PFLAG_VISIBLE_HANDLER_IS_SET).toLong() != 0L) {
        node.visibleHandler(visibleHandler)
      }
      if ((privateFlags and PFLAG_FOCUSED_HANDLER_IS_SET).toLong() != 0L) {
        node.focusedHandler(focusedHandler)
      }
      if ((privateFlags and PFLAG_FULL_IMPRESSION_HANDLER_IS_SET).toLong() != 0L) {
        node.fullImpressionHandler(fullImpressionHandler)
      }
      if ((privateFlags and PFLAG_INVISIBLE_HANDLER_IS_SET).toLong() != 0L) {
        node.invisibleHandler(invisibleHandler)
      }
      if ((privateFlags and PFLAG_UNFOCUSED_HANDLER_IS_SET).toLong() != 0L) {
        node.unfocusedHandler(unfocusedHandler)
      }
      if (privateFlags and PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET != 0) {
        node.visibilityChangedHandler(visibilityChangedHandler)
      }
      if ((privateFlags and PFLAG_TRANSITION_KEY_IS_SET).toLong() != 0L) {
        node.transitionKey(transitionKey, transitionOwnerKey)
      }
      if ((privateFlags and PFLAG_TRANSITION_KEY_TYPE_IS_SET).toLong() != 0L) {
        node.transitionKeyType(transitionKeyType)
      }
      if ((privateFlags and PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET).toLong() != 0L) {
        node.visibleHeightRatio(visibleHeightRatio)
      }
      if ((privateFlags and PFLAG_VISIBLE_WIDTH_RATIO_IS_SET).toLong() != 0L) {
        node.visibleWidthRatio(visibleWidthRatio)
      }
      if ((privateFlags and PFLAG_TOUCH_EXPANSION_IS_SET).toLong() != 0L) {
        touchExpansions?.let {
          for (i in 0 until Edges.EDGES_LENGTH) {
            val value = it.getRaw(i)
            if (!YogaConstants.isUndefined(value)) {
              node.touchExpansionPx(YogaEdge.fromInt(i), value.toInt())
            }
          }
        }
      }
      if ((privateFlags and PFLAG_BORDER_IS_SET).toLong() != 0L) {
        node.border(Preconditions.checkNotNull(border))
      }
      if ((privateFlags and PFLAG_STATE_LIST_ANIMATOR_IS_SET).toLong() != 0L) {
        node.stateListAnimator(stateListAnimator)
      }
      if ((privateFlags and PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET).toLong() != 0L) {
        node.stateListAnimatorRes(stateListAnimatorRes)
      }
      node.layerType(layerType, layerPaint)
      node.visibilityOutputTag(visibilityOutputTag)

      systemGestureExclusionZones?.let { node.addSystemGestureExclusionZones(it) }
    }

    override fun isEquivalentTo(other: OtherProps?): Boolean {
      if (this == other) {
        return true
      }
      return if (other == null) {
        false
      } else
          (privateFlags == other.privateFlags &&
              importantForAccessibility == other.importantForAccessibility &&
              duplicateParentState == other.duplicateParentState &&
              duplicateChildrenStates == other.duplicateChildrenStates &&
              stateListAnimatorRes == other.stateListAnimatorRes &&
              layerType == other.layerType &&
              other.visibleHeightRatio.compareTo(visibleHeightRatio) == 0 &&
              other.visibleWidthRatio.compareTo(visibleWidthRatio) == 0 &&
              equals(transitionKeyType, other.transitionKeyType) &&
              equals(stateListAnimator, other.stateListAnimator) &&
              equals(layerPaint, other.layerPaint) &&
              isEquivalentTo(visibleHandler, other.visibleHandler) &&
              isEquivalentTo(focusedHandler, other.focusedHandler) &&
              isEquivalentTo(unfocusedHandler, other.unfocusedHandler) &&
              isEquivalentTo(fullImpressionHandler, other.fullImpressionHandler) &&
              isEquivalentTo(invisibleHandler, other.invisibleHandler) &&
              isEquivalentTo(visibilityChangedHandler, other.visibilityChangedHandler) &&
              isEquivalentTo(touchExpansions, other.touchExpansions) &&
              isEquivalentTo(border, other.border) &&
              equals(transitionOwnerKey, other.transitionOwnerKey) &&
              equals(transitionKey, other.transitionKey) &&
              equals(systemGestureExclusionZones, other.systemGestureExclusionZones) &&
              DrawableUtils.isEquivalentTo(foreground, other.foreground))
    }

    companion object {
      // Flags used to indicate that a certain attribute was explicitly set on the node.
      private const val PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1 shl 0
      private const val PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1 shl 1
      private const val PFLAG_FOREGROUND_IS_SET = 1 shl 2
      private const val PFLAG_VISIBLE_HANDLER_IS_SET = 1 shl 3
      private const val PFLAG_FOCUSED_HANDLER_IS_SET = 1 shl 4
      private const val PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1 shl 5
      private const val PFLAG_INVISIBLE_HANDLER_IS_SET = 1 shl 6
      private const val PFLAG_UNFOCUSED_HANDLER_IS_SET = 1 shl 7
      private const val PFLAG_TOUCH_EXPANSION_IS_SET = 1 shl 8
      private const val PFLAG_TRANSITION_KEY_IS_SET = 1 shl 9
      private const val PFLAG_WRAP_IN_VIEW_IS_SET = 1 shl 10
      private const val PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET = 1 shl 11
      private const val PFLAG_VISIBLE_WIDTH_RATIO_IS_SET = 1 shl 12
      private const val PFLAG_BORDER_IS_SET = 1 shl 13
      private const val PFLAG_STATE_LIST_ANIMATOR_IS_SET = 1 shl 14
      private const val PFLAG_STATE_LIST_ANIMATOR_RES_IS_SET = 1 shl 15
      private const val PFLAG_VISIBILITY_CHANGED_HANDLER_IS_SET = 1 shl 16
      private const val PFLAG_TRANSITION_KEY_TYPE_IS_SET = 1 shl 17
      private const val PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET = 1 shl 18
    }
  }

  class DefaultLayoutProps : LayoutProps, Equivalence<DefaultLayoutProps?> {
    private var privateFlags: Int = 0

    @Px private var widthPx: Int = 0
    private var widthPercent: Float = 0f

    @Px private var minWidthPx: Int = 0
    private var minWidthPercent: Float = 0f

    @Px private var maxWidthPx: Int = 0
    private var maxWidthPercent: Float = 0f

    @Px private var heightPx: Int = 0
    private var heightPercent: Float = 0f

    @Px private var minHeightPx: Int = 0
    private var minHeightPercent: Float = 0f

    @Px private var maxHeightPx: Int = 0
    private var maxHeightPercent: Float = 0f
    private var flex: Float = 0f
    private var flexGrow: Float = 0f
    private var flexShrink: Float = 0f

    @Px private var flexBasisPx: Int = 0
    private var flexBasisPercent: Float = 0f
    private var aspectRatio: Float = 0f
    internal var layoutDirection: LayoutDirection? = null
      private set

    private var alignSelf: YogaAlign? = null
    private var positionType: YogaPositionType? = null
    private var positions: Edges? = null
    private var margins: Edges? = null
    private var marginPercents: Edges? = null
    private var marginAutos: MutableList<YogaEdge>? = null
    private var paddings: Edges? = null
    private var paddingPercents: Edges? = null
    private var positionPercents: Edges? = null
    private var isReferenceBaseline: Boolean = false
    private var useHeightAsBaseline: Boolean = false

    /** Used by [DebugLayoutNodeEditor] */
    private var borderEdges: Edges? = null
    private var gapGutter: YogaGutter? = null
    private var gapLength: Int = 0

    override fun widthPx(@Px width: Int) {
      privateFlags = privateFlags or PFLAG_WIDTH_IS_SET
      widthPx = width
    }

    override fun widthPercent(percent: Float) {
      privateFlags = privateFlags or PFLAG_WIDTH_PERCENT_IS_SET
      widthPercent = percent
    }

    override fun minWidthPx(@Px minWidth: Int) {
      privateFlags = privateFlags or PFLAG_MIN_WIDTH_IS_SET
      minWidthPx = minWidth
    }

    override fun maxWidthPx(@Px maxWidth: Int) {
      privateFlags = privateFlags or PFLAG_MAX_WIDTH_IS_SET
      maxWidthPx = maxWidth
    }

    override fun minWidthPercent(percent: Float) {
      privateFlags = privateFlags or PFLAG_MIN_WIDTH_PERCENT_IS_SET
      minWidthPercent = percent
    }

    override fun maxWidthPercent(percent: Float) {
      privateFlags = privateFlags or PFLAG_MAX_WIDTH_PERCENT_IS_SET
      maxWidthPercent = percent
    }

    override fun heightPx(@Px height: Int) {
      privateFlags = privateFlags or PFLAG_HEIGHT_IS_SET
      heightPx = height
    }

    override fun heightPercent(percent: Float) {
      privateFlags = privateFlags or PFLAG_HEIGHT_PERCENT_IS_SET
      heightPercent = percent
    }

    override fun minHeightPx(@Px minHeight: Int) {
      privateFlags = privateFlags or PFLAG_MIN_HEIGHT_IS_SET
      minHeightPx = minHeight
    }

    override fun maxHeightPx(@Px maxHeight: Int) {
      privateFlags = privateFlags or PFLAG_MAX_HEIGHT_IS_SET
      maxHeightPx = maxHeight
    }

    override fun minHeightPercent(percent: Float) {
      privateFlags = privateFlags or PFLAG_MIN_HEIGHT_PERCENT_IS_SET
      minHeightPercent = percent
    }

    override fun maxHeightPercent(percent: Float) {
      privateFlags = privateFlags or PFLAG_MAX_HEIGHT_PERCENT_IS_SET
      maxHeightPercent = percent
    }

    override fun layoutDirection(direction: LayoutDirection) {
      privateFlags = privateFlags or PFLAG_LAYOUT_DIRECTION_IS_SET
      layoutDirection = direction
    }

    override fun alignSelf(alignSelf: YogaAlign) {
      privateFlags = privateFlags or PFLAG_ALIGN_SELF_IS_SET
      this.alignSelf = alignSelf
    }

    override fun flex(flex: Float) {
      privateFlags = privateFlags or PFLAG_FLEX_IS_SET
      this.flex = flex
    }

    override fun flexGrow(flexGrow: Float) {
      privateFlags = privateFlags or PFLAG_FLEX_GROW_IS_SET
      this.flexGrow = flexGrow
    }

    override fun flexShrink(flexShrink: Float) {
      privateFlags = privateFlags or PFLAG_FLEX_SHRINK_IS_SET
      this.flexShrink = flexShrink
    }

    override fun flexBasisPx(@Px flexBasis: Int) {
      privateFlags = privateFlags or PFLAG_FLEX_BASIS_IS_SET
      flexBasisPx = flexBasis
    }

    override fun flexBasisPercent(percent: Float) {
      privateFlags = privateFlags or PFLAG_FLEX_BASIS_PERCENT_IS_SET
      flexBasisPercent = percent
    }

    override fun aspectRatio(aspectRatio: Float) {
      privateFlags = privateFlags or PFLAG_ASPECT_RATIO_IS_SET
      this.aspectRatio = aspectRatio
    }

    override fun positionType(positionType: YogaPositionType) {
      privateFlags = privateFlags or PFLAG_POSITION_TYPE_IS_SET
      this.positionType = positionType
    }

    override fun positionPx(edge: YogaEdge, @Px position: Int) {
      privateFlags = privateFlags or PFLAG_POSITION_IS_SET
      positions = (positions ?: Edges()).apply { this[edge] = position.toFloat() }
    }

    override fun positionPercent(edge: YogaEdge, percent: Float) {
      privateFlags = privateFlags or PFLAG_POSITION_PERCENT_IS_SET
      positionPercents = (positionPercents ?: Edges()).apply { this[edge] = percent }
    }

    override fun paddingPx(edge: YogaEdge, @Px padding: Int) {
      privateFlags = privateFlags or PFLAG_PADDING_IS_SET
      paddings = (paddings ?: Edges()).apply { this[edge] = padding.toFloat() }
    }

    override fun paddingPercent(edge: YogaEdge, percent: Float) {
      privateFlags = privateFlags or PFLAG_PADDING_PERCENT_IS_SET
      paddingPercents = (paddingPercents ?: Edges()).apply { this[edge] = percent }
    }

    override fun marginPx(edge: YogaEdge, @Px margin: Int) {
      privateFlags = privateFlags or PFLAG_MARGIN_IS_SET
      margins = (margins ?: Edges()).apply { this[edge] = margin.toFloat() }
    }

    override fun marginPercent(edge: YogaEdge, percent: Float) {
      privateFlags = privateFlags or PFLAG_MARGIN_PERCENT_IS_SET
      marginPercents = (marginPercents ?: Edges()).apply { this[edge] = percent }
    }

    override fun marginAuto(edge: YogaEdge) {
      privateFlags = privateFlags or PFLAG_MARGIN_AUTO_IS_SET
      marginAutos = (marginAutos ?: ArrayList(2)).apply { add(edge) }
    }

    override fun isReferenceBaseline(isReferenceBaseline: Boolean) {
      privateFlags = privateFlags or PFLAG_IS_REFERENCE_BASELINE_IS_SET
      this.isReferenceBaseline = isReferenceBaseline
    }

    override fun useHeightAsBaseline(useHeightAsBaseline: Boolean) {
      this.useHeightAsBaseline = useHeightAsBaseline
    }

    /** Used by [DebugLayoutNodeEditor] */
    override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) {
      borderEdges = (borderEdges ?: Edges()).apply { this[edge] = borderWidth }
    }

    override fun gap(gutter: YogaGutter, length: Int) {
      gapGutter = gutter
      gapLength = length
    }

    fun copyInto(target: LayoutProps) {
      if ((privateFlags and PFLAG_WIDTH_IS_SET).toLong() != 0L) {
        target.widthPx(widthPx)
      }
      if ((privateFlags and PFLAG_WIDTH_PERCENT_IS_SET).toLong() != 0L) {
        target.widthPercent(widthPercent)
      }
      if ((privateFlags and PFLAG_MIN_WIDTH_IS_SET).toLong() != 0L) {
        target.minWidthPx(minWidthPx)
      }
      if ((privateFlags and PFLAG_MIN_WIDTH_PERCENT_IS_SET).toLong() != 0L) {
        target.minWidthPercent(minWidthPercent)
      }
      if ((privateFlags and PFLAG_MAX_WIDTH_IS_SET).toLong() != 0L) {
        target.maxWidthPx(maxWidthPx)
      }
      if ((privateFlags and PFLAG_MAX_WIDTH_PERCENT_IS_SET).toLong() != 0L) {
        target.maxWidthPercent(maxWidthPercent)
      }
      if ((privateFlags and PFLAG_HEIGHT_IS_SET).toLong() != 0L) {
        target.heightPx(heightPx)
      }
      if ((privateFlags and PFLAG_HEIGHT_PERCENT_IS_SET).toLong() != 0L) {
        target.heightPercent(heightPercent)
      }
      if ((privateFlags and PFLAG_MIN_HEIGHT_IS_SET).toLong() != 0L) {
        target.minHeightPx(minHeightPx)
      }
      if ((privateFlags and PFLAG_MIN_HEIGHT_PERCENT_IS_SET).toLong() != 0L) {
        target.minHeightPercent(minHeightPercent)
      }
      if ((privateFlags and PFLAG_MAX_HEIGHT_IS_SET).toLong() != 0L) {
        target.maxHeightPx(maxHeightPx)
      }
      if ((privateFlags and PFLAG_MAX_HEIGHT_PERCENT_IS_SET).toLong() != 0L) {
        target.maxHeightPercent(maxHeightPercent)
      }
      if ((privateFlags and PFLAG_LAYOUT_DIRECTION_IS_SET).toLong() != 0L) {
        target.layoutDirection(Preconditions.checkNotNull(layoutDirection))
      }
      if ((privateFlags and PFLAG_ALIGN_SELF_IS_SET).toLong() != 0L) {
        target.alignSelf(Preconditions.checkNotNull(alignSelf))
      }
      if ((privateFlags and PFLAG_FLEX_IS_SET).toLong() != 0L) {
        target.flex(flex)
      }
      if ((privateFlags and PFLAG_FLEX_GROW_IS_SET).toLong() != 0L) {
        target.flexGrow(flexGrow)
      }
      if ((privateFlags and PFLAG_FLEX_SHRINK_IS_SET).toLong() != 0L) {
        target.flexShrink(flexShrink)
      }
      if ((privateFlags and PFLAG_FLEX_BASIS_IS_SET).toLong() != 0L) {
        target.flexBasisPx(flexBasisPx)
      }
      if ((privateFlags and PFLAG_FLEX_BASIS_PERCENT_IS_SET).toLong() != 0L) {
        target.flexBasisPercent(flexBasisPercent)
      }
      if ((privateFlags and PFLAG_ASPECT_RATIO_IS_SET).toLong() != 0L) {
        target.aspectRatio(aspectRatio)
      }
      if ((privateFlags and PFLAG_POSITION_TYPE_IS_SET).toLong() != 0L) {
        target.positionType(Preconditions.checkNotNull(positionType))
      }
      if ((privateFlags and PFLAG_POSITION_IS_SET).toLong() != 0L) {
        positions?.let {
          for (i in 0 until Edges.EDGES_LENGTH) {
            val value = it.getRaw(i)
            if (!YogaConstants.isUndefined(value)) {
              target.positionPx(YogaEdge.fromInt(i), value.toInt())
            }
          }
        }
      }
      if ((privateFlags and PFLAG_POSITION_PERCENT_IS_SET).toLong() != 0L) {
        positionPercents?.let {
          for (i in 0 until Edges.EDGES_LENGTH) {
            val value = it.getRaw(i)
            if (!YogaConstants.isUndefined(value)) {
              target.positionPercent(YogaEdge.fromInt(i), value)
            }
          }
        }
      }
      if ((privateFlags and PFLAG_PADDING_IS_SET).toLong() != 0L) {
        paddings?.let {
          for (i in 0 until Edges.EDGES_LENGTH) {
            val value = it.getRaw(i)
            if (!YogaConstants.isUndefined(value)) {
              target.paddingPx(YogaEdge.fromInt(i), value.toInt())
            }
          }
        }
      }
      if ((privateFlags and PFLAG_PADDING_PERCENT_IS_SET).toLong() != 0L) {
        paddingPercents?.let {
          for (i in 0 until Edges.EDGES_LENGTH) {
            val value = it.getRaw(i)
            if (!YogaConstants.isUndefined(value)) {
              target.paddingPercent(YogaEdge.fromInt(i), value)
            }
          }
        }
      }
      if ((privateFlags and PFLAG_MARGIN_IS_SET).toLong() != 0L) {
        margins?.let {
          for (i in 0 until Edges.EDGES_LENGTH) {
            val value = it.getRaw(i)
            if (!YogaConstants.isUndefined(value)) {
              target.marginPx(YogaEdge.fromInt(i), value.toInt())
            }
          }
        }
      }
      if ((privateFlags and PFLAG_MARGIN_PERCENT_IS_SET).toLong() != 0L) {
        marginPercents?.let {
          for (i in 0 until Edges.EDGES_LENGTH) {
            val value = it.getRaw(i)
            if (!YogaConstants.isUndefined(value)) {
              target.marginPercent(YogaEdge.fromInt(i), value)
            }
          }
        }
      }
      if ((privateFlags and PFLAG_MARGIN_AUTO_IS_SET).toLong() != 0L) {
        marginAutos?.forEach { target.marginAuto(it) }
      }
      if ((privateFlags and PFLAG_IS_REFERENCE_BASELINE_IS_SET).toLong() != 0L) {
        target.isReferenceBaseline(isReferenceBaseline)
      }
      if (useHeightAsBaseline) {
        target.useHeightAsBaseline(true)
      }
      borderEdges?.let {
        for (i in 0 until Edges.EDGES_LENGTH) {
          val value = it.getRaw(i)
          if (!YogaConstants.isUndefined(value)) {
            target.setBorderWidth(YogaEdge.fromInt(i), value)
          }
        }
      }
      gapGutter?.let { target.gap(it, gapLength) }
    }

    override fun isEquivalentTo(other: DefaultLayoutProps?): Boolean {
      if (this == other) {
        return true
      }
      return if (other == null) {
        false
      } else
          (privateFlags == other.privateFlags &&
              widthPx == other.widthPx &&
              other.widthPercent.compareTo(widthPercent) == 0 &&
              minWidthPx == other.minWidthPx &&
              other.minWidthPercent.compareTo(minWidthPercent) == 0 &&
              maxWidthPx == other.maxWidthPx &&
              other.maxWidthPercent.compareTo(maxWidthPercent) == 0 &&
              heightPx == other.heightPx &&
              other.heightPercent.compareTo(heightPercent) == 0 &&
              minHeightPx == other.minHeightPx &&
              other.minHeightPercent.compareTo(minHeightPercent) == 0 &&
              maxHeightPx == other.maxHeightPx &&
              other.maxHeightPercent.compareTo(maxHeightPercent) == 0 &&
              other.flex.compareTo(flex) == 0 &&
              other.flexGrow.compareTo(flexGrow) == 0 &&
              other.flexShrink.compareTo(flexShrink) == 0 &&
              flexBasisPx == other.flexBasisPx &&
              other.flexBasisPercent.compareTo(flexBasisPercent) == 0 &&
              other.aspectRatio.compareTo(aspectRatio) == 0 &&
              isReferenceBaseline == other.isReferenceBaseline &&
              useHeightAsBaseline == other.useHeightAsBaseline &&
              layoutDirection == other.layoutDirection &&
              alignSelf == other.alignSelf &&
              positionType == other.positionType &&
              isEquivalentTo(positions, other.positions) &&
              isEquivalentTo(margins, other.margins) &&
              isEquivalentTo(marginPercents, other.marginPercents) &&
              isEquivalentTo(paddings, other.paddings) &&
              isEquivalentTo(paddingPercents, other.paddingPercents) &&
              isEquivalentTo(positionPercents, other.positionPercents) &&
              isEquivalentTo(borderEdges, other.borderEdges) &&
              equals(marginAutos, other.marginAutos) &&
              equals(gapGutter, other.gapGutter) &&
              equals(gapLength, other.gapLength))
    }

    companion object {
      private const val PFLAG_WIDTH_IS_SET = 1 shl 0
      private const val PFLAG_WIDTH_PERCENT_IS_SET = 1 shl 1
      private const val PFLAG_MIN_WIDTH_IS_SET = 1 shl 2
      private const val PFLAG_MIN_WIDTH_PERCENT_IS_SET = 1 shl 3
      private const val PFLAG_MAX_WIDTH_IS_SET = 1 shl 4
      private const val PFLAG_MAX_WIDTH_PERCENT_IS_SET = 1 shl 5
      private const val PFLAG_HEIGHT_IS_SET = 1 shl 6
      private const val PFLAG_HEIGHT_PERCENT_IS_SET = 1 shl 7
      private const val PFLAG_MIN_HEIGHT_IS_SET = 1 shl 8
      private const val PFLAG_MIN_HEIGHT_PERCENT_IS_SET = 1 shl 9
      private const val PFLAG_MAX_HEIGHT_IS_SET = 1 shl 10
      private const val PFLAG_MAX_HEIGHT_PERCENT_IS_SET = 1 shl 11
      private const val PFLAG_LAYOUT_DIRECTION_IS_SET = 1 shl 12
      private const val PFLAG_ALIGN_SELF_IS_SET = 1 shl 13
      private const val PFLAG_FLEX_IS_SET = 1 shl 14
      private const val PFLAG_FLEX_GROW_IS_SET = 1 shl 15
      private const val PFLAG_FLEX_SHRINK_IS_SET = 1 shl 16
      private const val PFLAG_FLEX_BASIS_IS_SET = 1 shl 17
      private const val PFLAG_FLEX_BASIS_PERCENT_IS_SET = 1 shl 18
      private const val PFLAG_ASPECT_RATIO_IS_SET = 1 shl 19
      private const val PFLAG_POSITION_TYPE_IS_SET = 1 shl 20
      private const val PFLAG_POSITION_IS_SET = 1 shl 21
      private const val PFLAG_POSITION_PERCENT_IS_SET = 1 shl 22
      private const val PFLAG_PADDING_IS_SET = 1 shl 23
      private const val PFLAG_PADDING_PERCENT_IS_SET = 1 shl 24
      private const val PFLAG_MARGIN_IS_SET = 1 shl 25
      private const val PFLAG_MARGIN_PERCENT_IS_SET = 1 shl 26
      private const val PFLAG_MARGIN_AUTO_IS_SET = 1 shl 27
      private const val PFLAG_IS_REFERENCE_BASELINE_IS_SET = 1 shl 28
    }
  }

  companion object {
    // Flags used to indicate that a certain attribute was explicitly set on the node.
    private const val PFLAG_BACKGROUND_IS_SET = (1 shl 0)
    private const val PFLAG_TEST_KEY_IS_SET = (1 shl 1)
    private const val PFLAG_SCALE_KEY_IS_SET = (1 shl 2)
    private const val PFLAG_ALPHA_KEY_IS_SET = (1 shl 3)
    private const val PFLAG_ROTATION_KEY_IS_SET = (1 shl 4)
  }
}
