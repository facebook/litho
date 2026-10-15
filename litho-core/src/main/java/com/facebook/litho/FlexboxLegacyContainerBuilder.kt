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

@file:Suppress("DEPRECATION")

package com.facebook.litho

import android.animation.AnimatorInflater
import android.animation.StateListAnimator
import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.SparseArray
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import com.facebook.litho.accessibility.AccessibilityField
import com.facebook.litho.accessibility.ImportantForAccessibility
import com.facebook.litho.accessibility.accessibilityHeading
import com.facebook.litho.accessibility.accessibilityRole
import com.facebook.litho.accessibility.accessibilityRoleDescription
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.accessibility.importantForAccessibility
import com.facebook.litho.accessibility.screenReaderFocusable
import com.facebook.litho.accessibility.stateDescription
import com.facebook.litho.animated.alpha as dynamicAlpha
import com.facebook.litho.animated.background as dynamicBackground
import com.facebook.litho.animated.backgroundColor as dynamicBackgroundColor
import com.facebook.litho.animated.elevation as dynamicElevation
import com.facebook.litho.animated.foregroundColor as dynamicForegroundColor
import com.facebook.litho.animated.rotation as dynamicRotation
import com.facebook.litho.animated.rotationX as dynamicRotationX
import com.facebook.litho.animated.rotationY as dynamicRotationY
import com.facebook.litho.animated.scaleX as dynamicScaleX
import com.facebook.litho.animated.scaleY as dynamicScaleY
import com.facebook.litho.animated.translationX as dynamicTranslationX
import com.facebook.litho.animated.translationY as dynamicTranslationY
import com.facebook.litho.animated.translationZ as dynamicTranslationZ
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.androidStyle
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.margin
import com.facebook.litho.core.marginPercent
import com.facebook.litho.core.maxHeight
import com.facebook.litho.core.maxHeightPercent
import com.facebook.litho.core.maxWidth
import com.facebook.litho.core.maxWidthPercent
import com.facebook.litho.core.minHeight
import com.facebook.litho.core.minHeightPercent
import com.facebook.litho.core.minWidth
import com.facebook.litho.core.minWidthPercent
import com.facebook.litho.core.padding
import com.facebook.litho.core.paddingPercent
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.aspectRatio
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.isReferenceBaseline
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.flexbox.marginAuto
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionPercent
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.flexbox.useHeightAsBaseline
import com.facebook.litho.layout.flexbox.KColumn
import com.facebook.litho.layout.flexbox.KRow
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.view.ObjectField
import com.facebook.litho.view.addSystemGestureExclusionZone
import com.facebook.litho.view.alpha
import com.facebook.litho.view.background
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.clickable
import com.facebook.litho.view.clip
import com.facebook.litho.view.clipChildren
import com.facebook.litho.view.clipToOutline
import com.facebook.litho.view.componentTag
import com.facebook.litho.view.defaultFocusHighlightEnabled
import com.facebook.litho.view.duplicateChildrenStates
import com.facebook.litho.view.duplicateParentState
import com.facebook.litho.view.enabled
import com.facebook.litho.view.focusable
import com.facebook.litho.view.foreground
import com.facebook.litho.view.foregroundColor
import com.facebook.litho.view.keyboardNavigationCluster
import com.facebook.litho.view.layerType
import com.facebook.litho.view.rotation
import com.facebook.litho.view.rotationX
import com.facebook.litho.view.rotationY
import com.facebook.litho.view.scale
import com.facebook.litho.view.selected
import com.facebook.litho.view.stateListAnimator
import com.facebook.litho.view.testKey
import com.facebook.litho.view.tooltipText
import com.facebook.litho.view.touchExpansion
import com.facebook.litho.view.transitionName
import com.facebook.litho.view.viewId
import com.facebook.litho.view.viewTag
import com.facebook.litho.view.viewTags
import com.facebook.litho.view.wrapInView
import com.facebook.litho.visibility.VisibilityField
import com.facebook.litho.visibility.VisibilityFloatField
import com.facebook.litho.visibility.VisibilityFloatStyleItem
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap

/**
 * Base for the LEGACY [Row.Builder] / [Column.Builder]. It keeps the exact
 * `Component.ContainerBuilder` fluent surface (so every existing `Row.create(c)...build()` call
 * site — Java or Kotlin — compiles unchanged and so `Component.ContainerBuilder<?>` handles keep
 * working). The selected implementation is snapshotted when the builder is created: setters write
 * directly to the legacy component when the flag is off, and accumulate a [Style] only when the
 * flag is on.
 *
 * [build] is gated by [ComponentsConfiguration.useKComponentForLegacyRowColumn]:
 * - when ON, it constructs a [KRow] / [KColumn] carrying the accumulated [Style] (resolving via the
 *   launched `Style -> CommonProps` path — never a `CommonProps -> Style -> CommonProps` round
 *   trip);
 * - when OFF (default), it returns the backing [scratch] legacy SpecGenerated `Row`/`Column`, whose
 *   [CommonProps] were populated directly by the inherited builder setters.
 *
 * The [Component.Builder] machinery requires a [SpecGeneratedComponent] as its backing component;
 * it doubles as the [scratch] for the flag-OFF path.
 */
@SuppressLint(
    "IgKtViewHandleDetector",
) // Faithfully re-exposes the legacy builder's viewId/viewTag.
abstract class FlexboxLegacyContainerBuilder<T : Component.ContainerBuilder<T>>
protected constructor(
    private val context: ComponentContext,
    defStyleAttr: Int,
    defStyleRes: Int,
    private val scratch: SpecGeneratedComponent,
    private val customSimpleName: String?,
    private val buildKComponent: Boolean =
        ComponentsConfiguration.defaultInstance.useKComponentForLegacyRowColumn,
) :
    Component.ContainerBuilder<T>(
        context,
        if (buildKComponent) 0 else defStyleAttr,
        if (buildKComponent) 0 else defStyleRes,
        scratch,
    ) {

  private var style: Style =
      if (buildKComponent) Style.androidStyle(defStyleAttr, defStyleRes) else Style
  protected val children: MutableList<Component> = ArrayList()

  // Container props flow into the KRow/KColumn constructor (flag ON) or onto the legacy scratch
  // Row/Column (flag OFF), never into Style.
  protected var alignContentValue: YogaAlign? = null
  protected var alignItemsValue: YogaAlign? = null
  protected var justifyContentValue: YogaJustify? = null
  protected var wrapValue: YogaWrap? = null
  protected var reverseValue: Boolean = false
  private var gapValue: Dimen? = null
  protected var gapPxValue: Int? = null

  // Applied to the built component (not Style props).
  private var keyValue: String? = null
  private var handleValue: Handle? = null
  private var transitionKeyValue: String? = null
  private var transitionKeyTypeValue: Transition.TransitionKeyType? = null

  private var clickHandlerSet: Boolean = false
  private var backgroundSet: Boolean = false

  private inline fun styled(block: (Style) -> Style): T {
    style = block(style)
    return getThis()
  }

  private inline fun legacyOrStyled(legacy: () -> T, block: (Style) -> Style): T =
      if (buildKComponent) styled(block) else legacy()

  private inline fun self(block: () -> Unit): T {
    block()
    return getThis()
  }

  /**
   * Entry point for [Component.Builder.kotlinStyle]. The KComponent path accumulates the supplied
   * style; the legacy path applies it directly to the scratch component's [CommonProps].
   */
  internal fun mergeKotlinStyle(other: Style) {
    if (buildKComponent) {
      style += other
    } else {
      other.applyCommonProps(context, scratch.getOrCreateCommonProps())
    }
  }

  // region Container props ---------------------------------------------------

  override fun child(child: Component?): T = self { if (child != null) children.add(child) }

  override fun child(child: Component.Builder<*>?): T = self {
    if (child != null) children.add(child.build())
  }

  override fun children(children: List<Component>): T = self { this.children.addAll(children) }

  override fun alignContent(alignContent: YogaAlign?): T = self {
    alignContentValue = alignContent
  }

  override fun alignItems(alignItems: YogaAlign?): T = self { alignItemsValue = alignItems }

  override fun justifyContent(justifyContent: YogaJustify?): T = self {
    justifyContentValue = justifyContent
  }

  override fun wrap(wrap: YogaWrap?): T = self { wrapValue = wrap }

  override fun reverse(reverse: Boolean): T = self { reverseValue = reverse }

  override fun gapPx(gutter: YogaGutter, px: Int): T = self {
    require(gutter == YogaGutter.ALL) {
      "Only YogaGutter.ALL is supported on the legacy builder. Use the Kotlin DSL for per-axis gap."
    }
    gapValue = px.px
    gapPxValue = px
  }

  override fun gapDip(gutter: YogaGutter, dip: Float): T = self {
    require(gutter == YogaGutter.ALL) {
      "Only YogaGutter.ALL is supported on the legacy builder. Use the Kotlin DSL for per-axis gap."
    }
    gapValue = dip.dp
    gapPxValue = mResourceResolver.dipsToPixels(dip)
  }

  // endregion

  // region Identity ----------------------------------------------------------

  override fun key(key: String?): T =
      if (buildKComponent) self { keyValue = key ?: "null" } else super.key(key)

  override fun handle(handle: Handle?): T =
      if (buildKComponent) self { handleValue = handle } else super.handle(handle)

  override fun testKey(testKey: String?): T =
      legacyOrStyled({ super.testKey(testKey) }) { it.testKey(testKey) }

  override fun transitionName(transitionName: String?): T =
      legacyOrStyled({ super.transitionName(transitionName) }) {
        it.transitionName(transitionName)
      }

  override fun transitionKey(key: String?): T =
      if (buildKComponent) self { transitionKeyValue = key } else super.transitionKey(key)

  override fun transitionKeyType(type: Transition.TransitionKeyType): T =
      if (buildKComponent) self { transitionKeyTypeValue = type } else super.transitionKeyType(type)

  override fun viewId(id: Int): T = legacyOrStyled({ super.viewId(id) }) { it.viewId(id) }

  override fun viewTag(viewTag: Any?): T =
      legacyOrStyled({ super.viewTag(viewTag) }) { it.viewTag(viewTag) }

  override fun viewTags(viewTags: SparseArray<Any>?): T =
      if (buildKComponent) self { if (viewTags != null) style = style.viewTags(viewTags) }
      else super.viewTags(viewTags)

  override fun componentTag(componentTag: Any?): T =
      legacyOrStyled({ super.componentTag(componentTag) }) { it.componentTag(componentTag) }

  // endregion

  // region Size --------------------------------------------------------------

  override fun widthPx(width: Int): T =
      legacyOrStyled({ super.widthPx(width) }) { it.width(width.px) }

  override fun widthPercent(percent: Float): T =
      legacyOrStyled({ super.widthPercent(percent) }) { it.widthPercent(percent) }

  override fun heightPx(height: Int): T =
      legacyOrStyled({ super.heightPx(height) }) { it.height(height.px) }

  override fun heightPercent(percent: Float): T =
      legacyOrStyled({ super.heightPercent(percent) }) { it.heightPercent(percent) }

  override fun minWidthPx(minWidth: Int): T =
      legacyOrStyled({ super.minWidthPx(minWidth) }) { it.minWidth(minWidth.px) }

  override fun minWidthPercent(percent: Float): T =
      legacyOrStyled({ super.minWidthPercent(percent) }) { it.minWidthPercent(percent) }

  override fun maxWidthPx(maxWidth: Int): T =
      legacyOrStyled({ super.maxWidthPx(maxWidth) }) { it.maxWidth(maxWidth.px) }

  override fun maxWidthPercent(percent: Float): T =
      legacyOrStyled({ super.maxWidthPercent(percent) }) { it.maxWidthPercent(percent) }

  override fun minHeightPx(minHeight: Int): T =
      legacyOrStyled({ super.minHeightPx(minHeight) }) { it.minHeight(minHeight.px) }

  override fun minHeightPercent(percent: Float): T =
      legacyOrStyled({ super.minHeightPercent(percent) }) { it.minHeightPercent(percent) }

  override fun maxHeightPx(maxHeight: Int): T =
      legacyOrStyled({ super.maxHeightPx(maxHeight) }) { it.maxHeight(maxHeight.px) }

  override fun maxHeightPercent(percent: Float): T =
      legacyOrStyled({ super.maxHeightPercent(percent) }) { it.maxHeightPercent(percent) }

  override fun aspectRatio(aspectRatio: Float): T =
      legacyOrStyled({ super.aspectRatio(aspectRatio) }) { it.aspectRatio(aspectRatio) }

  // endregion

  // region Spacing (margin / padding) ---------------------------------------

  override fun marginPx(edge: YogaEdge, margin: Int): T =
      legacyOrStyled({ super.marginPx(edge, margin) }) {
        applyMargin(it, edge, margin.px)
      }

  override fun marginPercent(edge: YogaEdge, percent: Float): T =
      legacyOrStyled({ super.marginPercent(edge, percent) }) {
        applyMarginPercent(it, edge, percent)
      }

  override fun marginAuto(edge: YogaEdge): T =
      legacyOrStyled({ super.marginAuto(edge) }) { it.marginAuto(edge) }

  override fun paddingPx(edge: YogaEdge, padding: Int): T =
      legacyOrStyled({ super.paddingPx(edge, padding) }) {
        applyPadding(it, edge, padding.px)
      }

  override fun paddingPercent(edge: YogaEdge, percent: Float): T =
      legacyOrStyled({ super.paddingPercent(edge, percent) }) {
        applyPaddingPercent(it, edge, percent)
      }

  @SuppressLint("KComponentNonRtlParamsUsageDetector")
  private fun applyMargin(s: Style, edge: YogaEdge, value: Dimen): Style =
      when (edge) {
        YogaEdge.LEFT -> s.margin(left = value)
        YogaEdge.TOP -> s.margin(top = value)
        YogaEdge.RIGHT -> s.margin(right = value)
        YogaEdge.BOTTOM -> s.margin(bottom = value)
        YogaEdge.START -> s.margin(start = value)
        YogaEdge.END -> s.margin(end = value)
        YogaEdge.HORIZONTAL -> s.margin(horizontal = value)
        YogaEdge.VERTICAL -> s.margin(vertical = value)
        YogaEdge.ALL -> s.margin(all = value)
      }

  @SuppressLint("KComponentNonRtlParamsUsageDetector")
  private fun applyMarginPercent(s: Style, edge: YogaEdge, value: Float): Style =
      when (edge) {
        YogaEdge.LEFT -> s.marginPercent(left = value)
        YogaEdge.TOP -> s.marginPercent(top = value)
        YogaEdge.RIGHT -> s.marginPercent(right = value)
        YogaEdge.BOTTOM -> s.marginPercent(bottom = value)
        YogaEdge.START -> s.marginPercent(start = value)
        YogaEdge.END -> s.marginPercent(end = value)
        YogaEdge.HORIZONTAL -> s.marginPercent(horizontal = value)
        YogaEdge.VERTICAL -> s.marginPercent(vertical = value)
        YogaEdge.ALL -> s.marginPercent(all = value)
      }

  @SuppressLint("KComponentNonRtlParamsUsageDetector")
  private fun applyPadding(s: Style, edge: YogaEdge, value: Dimen): Style =
      when (edge) {
        YogaEdge.LEFT -> s.padding(left = value)
        YogaEdge.TOP -> s.padding(top = value)
        YogaEdge.RIGHT -> s.padding(right = value)
        YogaEdge.BOTTOM -> s.padding(bottom = value)
        YogaEdge.START -> s.padding(start = value)
        YogaEdge.END -> s.padding(end = value)
        YogaEdge.HORIZONTAL -> s.padding(horizontal = value)
        YogaEdge.VERTICAL -> s.padding(vertical = value)
        YogaEdge.ALL -> s.padding(all = value)
      }

  @SuppressLint("KComponentNonRtlParamsUsageDetector")
  private fun applyPaddingPercent(s: Style, edge: YogaEdge, value: Float): Style =
      when (edge) {
        YogaEdge.LEFT -> s.paddingPercent(left = value)
        YogaEdge.TOP -> s.paddingPercent(top = value)
        YogaEdge.RIGHT -> s.paddingPercent(right = value)
        YogaEdge.BOTTOM -> s.paddingPercent(bottom = value)
        YogaEdge.START -> s.paddingPercent(start = value)
        YogaEdge.END -> s.paddingPercent(end = value)
        YogaEdge.HORIZONTAL -> s.paddingPercent(horizontal = value)
        YogaEdge.VERTICAL -> s.paddingPercent(vertical = value)
        YogaEdge.ALL -> s.paddingPercent(all = value)
      }

  // endregion

  // region Position ----------------------------------------------------------

  override fun positionPx(edge: YogaEdge, position: Int): T =
      legacyOrStyled({ super.positionPx(edge, position) }) {
        applyPosition(it, edge, position.px)
      }

  override fun positionPercent(edge: YogaEdge, percent: Float): T =
      legacyOrStyled({ super.positionPercent(edge, percent) }) {
        applyPositionPercent(it, edge, percent)
      }

  override fun positionType(positionType: YogaPositionType): T =
      legacyOrStyled({ super.positionType(positionType) }) {
        it.positionType(positionType)
      }

  override fun layoutDirection(direction: YogaDirection): T =
      legacyOrStyled({ super.layoutDirection(direction) }) {
        it.layoutDirection(direction)
      }

  @SuppressLint("KComponentNonRtlParamsUsageDetector")
  private fun applyPosition(s: Style, edge: YogaEdge, value: Dimen): Style =
      when (edge) {
        YogaEdge.LEFT -> s.position(left = value)
        YogaEdge.TOP -> s.position(top = value)
        YogaEdge.RIGHT -> s.position(right = value)
        YogaEdge.BOTTOM -> s.position(bottom = value)
        YogaEdge.START -> s.position(start = value)
        YogaEdge.END -> s.position(end = value)
        YogaEdge.HORIZONTAL -> s.position(horizontal = value)
        YogaEdge.VERTICAL -> s.position(vertical = value)
        YogaEdge.ALL -> s.position(all = value)
      }

  @SuppressLint("KComponentNonRtlParamsUsageDetector")
  private fun applyPositionPercent(s: Style, edge: YogaEdge, value: Float): Style =
      when (edge) {
        YogaEdge.LEFT -> s.positionPercent(left = value)
        YogaEdge.TOP -> s.positionPercent(top = value)
        YogaEdge.RIGHT -> s.positionPercent(right = value)
        YogaEdge.BOTTOM -> s.positionPercent(bottom = value)
        YogaEdge.START -> s.positionPercent(start = value)
        YogaEdge.END -> s.positionPercent(end = value)
        YogaEdge.HORIZONTAL -> s.positionPercent(horizontal = value)
        YogaEdge.VERTICAL -> s.positionPercent(vertical = value)
        YogaEdge.ALL -> s.positionPercent(all = value)
      }

  // endregion

  // region Flex --------------------------------------------------------------

  /**
   * Yoga's `flex` shorthand is a different field from `flexGrow`, so the split by sign is load
   * bearing. A positive value is equivalent either way here: `Node.resolveFlexGrow` falls back to
   * `flex`, and because Litho's config sets `useWebDefaults`
   * ([com.facebook.litho.yoga.LithoYogaFactory.createYogaConfig]) `Node.processFlexBasis` leaves
   * basis at `auto` for a positive `flex` rather than pinning it to `0px`. A negative value is not:
   * Yoga reads it as shrink, and only via the `!useWebDefaults` branch of `resolveFlexShrink`,
   * which Litho never takes — so passing it to `grow` would leave a negative grow factor on the
   * node and stretch the child instead of leaving it at its content size.
   */
  override fun flex(flex: Float): T =
      legacyOrStyled({ super.flex(flex) }) {
        if (flex < 0f) it.flex(shrink = -flex) else it.flex(grow = flex)
      }

  override fun flexGrow(flexGrow: Float): T =
      legacyOrStyled({ super.flexGrow(flexGrow) }) { it.flex(grow = flexGrow) }

  override fun flexShrink(flexShrink: Float): T =
      legacyOrStyled({ super.flexShrink(flexShrink) }) { it.flex(shrink = flexShrink) }

  override fun flexBasisPx(flexBasis: Int): T =
      legacyOrStyled({ super.flexBasisPx(flexBasis) }) { it.flex(basis = flexBasis.px) }

  override fun flexBasisPercent(percent: Float): T =
      legacyOrStyled({ super.flexBasisPercent(percent) }) { it.flex(basisPercent = percent) }

  override fun alignSelf(alignSelf: YogaAlign): T =
      legacyOrStyled({ super.alignSelf(alignSelf) }) { it.alignSelf(alignSelf) }

  override fun isReferenceBaseline(isReferenceBaseline: Boolean): T =
      legacyOrStyled({ super.isReferenceBaseline(isReferenceBaseline) }) {
        it.isReferenceBaseline(isReferenceBaseline)
      }

  override fun useHeightAsBaseline(useHeightAsBaseline: Boolean): T =
      legacyOrStyled({ super.useHeightAsBaseline(useHeightAsBaseline) }) {
        it.useHeightAsBaseline(useHeightAsBaseline)
      }

  // endregion

  // region Visual ------------------------------------------------------------

  override fun background(background: Drawable?): T =
      legacyOrStyled({ super.background(background) }) { s ->
        backgroundSet = background != null
        s.background(background)
      }

  override fun backgroundColor(@ColorInt backgroundColor: Int): T =
      legacyOrStyled({ super.backgroundColor(backgroundColor) }) { s ->
        backgroundSet = true
        s.backgroundColor(backgroundColor)
      }

  override fun backgroundColor(value: DynamicValue<Int>?): T =
      legacyOrStyled({ super.backgroundColor(value) }) { s ->
        backgroundSet = value != null
        if (value != null) s.dynamicBackgroundColor(value) else s
      }

  // These override Java params that are @Nullable because the base has always tolerated null (it
  // just stores the value). A non-null Kotlin override would add an
  // Intrinsics.checkNotNullParameter
  // the base never had, and it runs before the flag is read -- so it would throw on the default
  // flag-OFF path too, turning a working `.scaleX(maybeNull)` call site into an NPE.
  override fun backgroundDynamicDrawable(value: DynamicValue<out Drawable>?): T =
      legacyOrStyled({ super.backgroundDynamicDrawable(value) }) { s ->
        backgroundSet = value != null
        if (value != null) s.dynamicBackground(value) else s
      }

  override fun foreground(foreground: Drawable?): T =
      legacyOrStyled({ super.foreground(foreground) }) { it.foreground(foreground) }

  override fun foregroundColor(@ColorInt foregroundColor: Int): T =
      legacyOrStyled({ super.foregroundColor(foregroundColor) }) {
        it.foregroundColor(foregroundColor)
      }

  override fun foregroundColor(value: DynamicValue<Int>?): T =
      legacyOrStyled({ super.foregroundColor(value) }) {
        if (value != null) it.dynamicForegroundColor(value) else it
      }

  override fun border(border: Border?): T =
      legacyOrStyled({ super.border(border) }) { if (border != null) it.border(border) else it }

  override fun alpha(alpha: Float): T = legacyOrStyled({ super.alpha(alpha) }) { it.alpha(alpha) }

  override fun alpha(value: DynamicValue<Float>?): T =
      legacyOrStyled({ super.alpha(value) }) {
        if (value != null) it.dynamicAlpha(value) else it
      }

  override fun rotation(rotation: Float): T =
      legacyOrStyled({ super.rotation(rotation) }) { it.rotation(rotation) }

  override fun rotation(rotation: DynamicValue<Float>?): T =
      legacyOrStyled({ super.rotation(rotation) }) {
        if (rotation != null) it.dynamicRotation(rotation) else it
      }

  override fun rotationX(rotationX: Float): T =
      legacyOrStyled({ super.rotationX(rotationX) }) { it.rotationX(rotationX) }

  override fun rotationX(rotationX: DynamicValue<Float>?): T =
      legacyOrStyled({ super.rotationX(rotationX) }) {
        if (rotationX != null) it.dynamicRotationX(rotationX) else it
      }

  override fun rotationY(rotationY: Float): T =
      legacyOrStyled({ super.rotationY(rotationY) }) { it.rotationY(rotationY) }

  override fun rotationY(rotationY: DynamicValue<Float>?): T =
      legacyOrStyled({ super.rotationY(rotationY) }) {
        if (rotationY != null) it.dynamicRotationY(rotationY) else it
      }

  override fun scale(scale: Float): T = legacyOrStyled({ super.scale(scale) }) { it.scale(scale) }

  override fun scaleX(value: DynamicValue<Float>?): T =
      legacyOrStyled({ super.scaleX(value) }) {
        if (value != null) it.dynamicScaleX(value) else it
      }

  override fun scaleY(value: DynamicValue<Float>?): T =
      legacyOrStyled({ super.scaleY(value) }) {
        if (value != null) it.dynamicScaleY(value) else it
      }

  override fun translationX(value: DynamicValue<Float>?): T =
      legacyOrStyled({ super.translationX(value) }) {
        if (value != null) it.dynamicTranslationX(value) else it
      }

  override fun translationY(value: DynamicValue<Float>?): T =
      legacyOrStyled({ super.translationY(value) }) {
        if (value != null) it.dynamicTranslationY(value) else it
      }

  override fun translationZ(value: DynamicValue<Float>?): T =
      legacyOrStyled({ super.translationZ(value) }) {
        if (value != null) it.dynamicTranslationZ(value) else it
      }

  override fun shadowElevationPx(shadowElevation: Float): T =
      legacyOrStyled({ super.shadowElevationPx(shadowElevation) }) {
        it +
            LegacyCommonPropStyleItem(
                LegacyCommonPropField.SHADOW_ELEVATION_PX,
                shadowElevation,
            )
      }

  override fun shadowElevation(value: DynamicValue<Float>?): T =
      legacyOrStyled({ super.shadowElevation(value) }) {
        if (value != null) it.dynamicElevation(value) else it
      }

  override fun outlineProvider(outlineProvider: ViewOutlineProvider?): T =
      legacyOrStyled({ super.outlineProvider(outlineProvider) }) {
        it.clip(outlineProvider)
      }

  override fun clipToOutline(clipToOutline: Boolean): T =
      legacyOrStyled({ super.clipToOutline(clipToOutline) }) { it.clipToOutline(clipToOutline) }

  override fun clipChildren(clipChildren: Boolean): T =
      legacyOrStyled({ super.clipChildren(clipChildren) }) { it.clipChildren(clipChildren) }

  override fun stateListAnimator(stateListAnimator: StateListAnimator?): T =
      legacyOrStyled({ super.stateListAnimator(stateListAnimator) }) {
        it.stateListAnimator(stateListAnimator)
      }

  override fun stateListAnimatorRes(resId: Int): T =
      when {
        !buildKComponent -> super.stateListAnimatorRes(resId)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
            stateListAnimator(AnimatorInflater.loadStateListAnimator(context.androidContext, resId))
        else ->
            styled {
              it +
                  LegacyCommonPropStyleItem(
                      LegacyCommonPropField.STATE_LIST_ANIMATOR_RES,
                      resId,
                  )
            }
      }

  override fun layerType(type: Int, paint: Paint?): T =
      legacyOrStyled({ super.layerType(type, paint) }) { it.layerType(type, paint) }

  override fun wrapInView(): T = legacyOrStyled({ super.wrapInView() }) { it.wrapInView() }

  // Legacy YogaEdge maps to the matching physical Style param; start/end would change RTL behavior.
  @SuppressLint("KComponentNonRtlParamsUsageDetector")
  override fun touchExpansionPx(edge: YogaEdge, touchExpansion: Int): T =
      legacyOrStyled({ super.touchExpansionPx(edge, touchExpansion) }) { s ->
        val value = touchExpansion.px
        when (edge) {
          YogaEdge.LEFT -> s.touchExpansion(left = value)
          YogaEdge.TOP -> s.touchExpansion(top = value)
          YogaEdge.RIGHT -> s.touchExpansion(right = value)
          YogaEdge.BOTTOM -> s.touchExpansion(bottom = value)
          YogaEdge.START -> s.touchExpansion(start = value)
          YogaEdge.END -> s.touchExpansion(end = value)
          YogaEdge.HORIZONTAL -> s.touchExpansion(horizontal = value)
          YogaEdge.VERTICAL -> s.touchExpansion(vertical = value)
          YogaEdge.ALL -> s.touchExpansion(all = value)
        }
      }

  // endregion

  // region Accessibility / state --------------------------------------------

  override fun contentDescription(contentDescription: CharSequence?): T =
      legacyOrStyled({ super.contentDescription(contentDescription) }) {
        it.contentDescription(contentDescription)
      }

  override fun accessibilityRole(accessibilityRole: String?): T =
      legacyOrStyled({ super.accessibilityRole(accessibilityRole) }) {
        it.accessibilityRole(accessibilityRole)
      }

  override fun accessibilityRoleDescription(roleDescription: CharSequence?): T =
      legacyOrStyled({ super.accessibilityRoleDescription(roleDescription) }) {
        it.accessibilityRoleDescription(roleDescription)
      }

  override fun accessibilityHeading(isHeading: Boolean): T =
      legacyOrStyled({ super.accessibilityHeading(isHeading) }) {
        it.accessibilityHeading(isHeading)
      }

  override fun stateDescription(stateDescription: CharSequence?): T =
      legacyOrStyled({ super.stateDescription(stateDescription) }) {
        it.stateDescription(stateDescription)
      }

  override fun importantForAccessibility(importantForAccessibility: Int): T =
      legacyOrStyled({ super.importantForAccessibility(importantForAccessibility) }) {
        it.importantForAccessibility(toImportantForAccessibility(importantForAccessibility))
      }

  override fun focusable(isFocusable: Boolean): T =
      legacyOrStyled({ super.focusable(isFocusable) }) { it.focusable(isFocusable) }

  override fun screenReaderFocusable(isFocusable: Boolean): T =
      legacyOrStyled({ super.screenReaderFocusable(isFocusable) }) {
        it.screenReaderFocusable(isFocusable)
      }

  override fun enabled(isEnabled: Boolean): T =
      legacyOrStyled({ super.enabled(isEnabled) }) { it.enabled(isEnabled) }

  override fun clickable(isClickable: Boolean): T =
      legacyOrStyled({ super.clickable(isClickable) }) { it.clickable(isClickable) }

  override fun selected(isSelected: Boolean): T =
      legacyOrStyled({ super.selected(isSelected) }) { it.selected(isSelected) }

  override fun keyboardNavigationCluster(isKeyboardNavigationCluster: Boolean): T =
      legacyOrStyled({ super.keyboardNavigationCluster(isKeyboardNavigationCluster) }) {
        it.keyboardNavigationCluster(isKeyboardNavigationCluster)
      }

  override fun defaultFocusHighlightEnabled(defaultFocusHighlightEnabled: Boolean): T =
      legacyOrStyled({ super.defaultFocusHighlightEnabled(defaultFocusHighlightEnabled) }) {
        it.defaultFocusHighlightEnabled(defaultFocusHighlightEnabled)
      }

  override fun duplicateParentState(duplicateParentState: Boolean): T =
      legacyOrStyled({ super.duplicateParentState(duplicateParentState) }) {
        it.duplicateParentState(duplicateParentState)
      }

  override fun duplicateChildrenStates(duplicateChildrenStates: Boolean): T =
      legacyOrStyled({ super.duplicateChildrenStates(duplicateChildrenStates) }) {
        it.duplicateChildrenStates(duplicateChildrenStates)
      }

  override fun tooltipText(tooltipText: String?): T =
      legacyOrStyled({ super.tooltipText(tooltipText) }) { it.tooltipText(tooltipText) }

  private fun toImportantForAccessibility(value: Int): ImportantForAccessibility =
      when (value) {
        1 -> ImportantForAccessibility.YES
        2 -> ImportantForAccessibility.NO
        4 -> ImportantForAccessibility.NO_HIDE_DESCENDANTS
        else -> ImportantForAccessibility.AUTO
      }

  // endregion

  // region Event handlers ----------------------------------------------------

  // The Kotlin Style APIs accept callbacks and create new EventHandlers. Legacy builders already
  // receive EventHandlers, so the KComponent path carries those handlers through unchanged.
  private fun <E> Style.withHandler(
      field: StyleItemField,
      handler: EventHandler<E>?,
  ): Style = this + RawEventHandlerStyleItem(field, handler)

  private fun <E> Style.withVisibilityHandler(
      field: VisibilityField,
      handler: EventHandler<E>?,
      visibleWidthRatio: Float,
      visibleHeightRatio: Float,
  ): Style {
    var result = withHandler(field, handler)
    if (visibleWidthRatio != 0f) {
      result +=
          VisibilityFloatStyleItem(VisibilityFloatField.VISIBLE_WIDTH_RATIO, visibleWidthRatio)
    }
    if (visibleHeightRatio != 0f) {
      result +=
          VisibilityFloatStyleItem(VisibilityFloatField.VISIBLE_HEIGHT_RATIO, visibleHeightRatio)
    }
    return result
  }

  override fun clickHandler(clickHandler: EventHandler<ClickEvent>?): T =
      legacyOrStyled({ super.clickHandler(clickHandler) }) { style ->
        clickHandlerSet = clickHandler != null
        style.withHandler(ObjectField.ON_CLICK, clickHandler)
      }

  override fun longClickHandler(longClickHandler: EventHandler<LongClickEvent>?): T =
      legacyOrStyled({ super.longClickHandler(longClickHandler) }) {
        it.withHandler(ObjectField.ON_LONG_CLICK, longClickHandler)
      }

  override fun touchHandler(touchHandler: EventHandler<TouchEvent>?): T =
      legacyOrStyled({ super.touchHandler(touchHandler) }) {
        it.withHandler(ObjectField.ON_TOUCH, touchHandler)
      }

  override fun interceptTouchHandler(interceptTouchHandler: EventHandler<InterceptTouchEvent>?): T =
      legacyOrStyled({ super.interceptTouchHandler(interceptTouchHandler) }) {
        it.withHandler(ObjectField.ON_INTERCEPT_TOUCH, interceptTouchHandler)
      }

  override fun focusChangeHandler(focusChangeHandler: EventHandler<FocusChangedEvent>?): T =
      legacyOrStyled({ super.focusChangeHandler(focusChangeHandler) }) {
        it.withHandler(ObjectField.ON_FOCUS_CHANGED, focusChangeHandler)
      }

  override fun visibleHandler(
      visibleWidthRatio: Float,
      visibleHeightRatio: Float,
      visibleHandler: EventHandler<com.facebook.rendercore.visibility.VisibleEvent>?,
  ): T = legacyOrStyled(
      {
        super.visibleHandler(visibleWidthRatio, visibleHeightRatio, visibleHandler)
      },
  ) {
    it.withVisibilityHandler(
        VisibilityField.ON_VISIBLE,
        visibleHandler,
        visibleWidthRatio,
        visibleHeightRatio,
    )
  }

  override fun invisibleHandler(
      visibleWidthRatio: Float,
      visibleHeightRatio: Float,
      invisibleHandler: EventHandler<com.facebook.rendercore.visibility.InvisibleEvent>?,
  ): T = legacyOrStyled(
      {
        super.invisibleHandler(visibleWidthRatio, visibleHeightRatio, invisibleHandler)
      },
  ) {
    it.withVisibilityHandler(
        VisibilityField.ON_INVISIBLE,
        invisibleHandler,
        visibleWidthRatio,
        visibleHeightRatio,
    )
  }

  override fun focusedHandler(
      visibleWidthRatio: Float,
      visibleHeightRatio: Float,
      focusedHandler: EventHandler<com.facebook.rendercore.visibility.FocusedVisibleEvent>?,
  ): T = legacyOrStyled(
      {
        super.focusedHandler(visibleWidthRatio, visibleHeightRatio, focusedHandler)
      },
  ) {
    it.withVisibilityHandler(
        VisibilityField.ON_FOCUSED,
        focusedHandler,
        visibleWidthRatio,
        visibleHeightRatio,
    )
  }

  override fun unfocusedHandler(
      visibleWidthRatio: Float,
      visibleHeightRatio: Float,
      unfocusedHandler: EventHandler<com.facebook.rendercore.visibility.UnfocusedVisibleEvent>?,
  ): T = legacyOrStyled(
      {
        super.unfocusedHandler(visibleWidthRatio, visibleHeightRatio, unfocusedHandler)
      },
  ) {
    it.withVisibilityHandler(
        VisibilityField.ON_UNFOCUSED,
        unfocusedHandler,
        visibleWidthRatio,
        visibleHeightRatio,
    )
  }

  override fun fullImpressionHandler(
      visibleWidthRatio: Float,
      visibleHeightRatio: Float,
      fullImpressionHandler:
          EventHandler<com.facebook.rendercore.visibility.FullImpressionVisibleEvent>?,
  ): T = legacyOrStyled(
      {
        super.fullImpressionHandler(visibleWidthRatio, visibleHeightRatio, fullImpressionHandler)
      },
  ) {
    it.withVisibilityHandler(
        VisibilityField.ON_FULL_IMPRESSION,
        fullImpressionHandler,
        visibleWidthRatio,
        visibleHeightRatio,
    )
  }

  override fun visibilityChangedHandler(
      visibleWidthRatio: Float,
      visibleHeightRatio: Float,
      visibilityChangedHandler:
          EventHandler<com.facebook.rendercore.visibility.VisibilityChangedEvent>?,
  ): T = legacyOrStyled(
      {
        super.visibilityChangedHandler(
            visibleWidthRatio,
            visibleHeightRatio,
            visibilityChangedHandler,
        )
      },
  ) {
    it.withVisibilityHandler(
        VisibilityField.ON_VISIBILITY_CHANGED,
        visibilityChangedHandler,
        visibleWidthRatio,
        visibleHeightRatio,
    )
  }

  override fun onInitializeAccessibilityNodeInfoHandler(
      onInitializeAccessibilityNodeInfoHandler:
          EventHandler<OnInitializeAccessibilityNodeInfoEvent>?,
  ): T = legacyOrStyled(
      {
        super.onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler)
      },
  ) {
    it.withHandler(
        AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_NODE_INFO,
        onInitializeAccessibilityNodeInfoHandler,
    )
  }

  override fun onInitializeAccessibilityEventHandler(
      onInitializeAccessibilityEventHandler: EventHandler<OnInitializeAccessibilityEventEvent>?,
  ): T = legacyOrStyled(
      {
        super.onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler)
      },
  ) {
    it.withHandler(
        AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_EVENT,
        onInitializeAccessibilityEventHandler,
    )
  }

  override fun onPopulateAccessibilityEventHandler(
      onPopulateAccessibilityEventHandler: EventHandler<OnPopulateAccessibilityEventEvent>?,
  ): T = legacyOrStyled(
      {
        super.onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler)
      },
  ) {
    it.withHandler(
        AccessibilityField.ON_POPULATE_ACCESSIBILITY_EVENT,
        onPopulateAccessibilityEventHandler,
    )
  }

  override fun onRequestSendAccessibilityEventHandler(
      onRequestSendAccessibilityEventHandler: EventHandler<OnRequestSendAccessibilityEventEvent>?,
  ): T = legacyOrStyled(
      {
        super.onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler)
      },
  ) {
    it.withHandler(
        AccessibilityField.ON_REQUEST_SEND_ACCESSIBILITY_EVENT,
        onRequestSendAccessibilityEventHandler,
    )
  }

  override fun performAccessibilityActionHandler(
      performAccessibilityActionHandler: EventHandler<PerformAccessibilityActionEvent>?,
  ): T = legacyOrStyled(
      {
        super.performAccessibilityActionHandler(performAccessibilityActionHandler)
      },
  ) {
    it.withHandler(
        AccessibilityField.PERFORM_ACCESSIBILITY_ACTION,
        performAccessibilityActionHandler,
    )
  }

  override fun sendAccessibilityEventHandler(
      sendAccessibilityEventHandler: EventHandler<SendAccessibilityEventEvent>?,
  ): T =
      legacyOrStyled({ super.sendAccessibilityEventHandler(sendAccessibilityEventHandler) }) {
        it.withHandler(
            AccessibilityField.SEND_ACCESSIBILITY_EVENT,
            sendAccessibilityEventHandler,
        )
      }

  override fun sendAccessibilityEventUncheckedHandler(
      sendAccessibilityEventUncheckedHandler: EventHandler<SendAccessibilityEventUncheckedEvent>?,
  ): T = legacyOrStyled(
      {
        super.sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler)
      },
  ) {
    it.withHandler(
        AccessibilityField.SEND_ACCESSIBILITY_EVENT_UNCHECKED,
        sendAccessibilityEventUncheckedHandler,
    )
  }

  override fun onPerformActionForVirtualViewHandler(
      onPerformActionForVirtualViewHandler: EventHandler<PerformActionForVirtualViewEvent>?,
  ): T = legacyOrStyled(
      {
        super.onPerformActionForVirtualViewHandler(onPerformActionForVirtualViewHandler)
      },
  ) {
    it.withHandler(
        AccessibilityField.ON_PERFORM_ACTION_FOR_VIRTUAL_VIEW,
        onPerformActionForVirtualViewHandler,
    )
  }

  override fun onVirtualViewKeyboardFocusChangedHandler(
      onVirtualViewKeyboardFocusChangedHandler: EventHandler<VirtualViewKeyboardFocusChangedEvent>?,
  ): T = legacyOrStyled(
      {
        super.onVirtualViewKeyboardFocusChangedHandler(onVirtualViewKeyboardFocusChangedHandler)
      },
  ) {
    it.withHandler(
        AccessibilityField.ON_VIRTUAL_VIEW_KEYBOARD_FOCUS_CHANGED,
        onVirtualViewKeyboardFocusChangedHandler,
    )
  }

  override fun dispatchPopulateAccessibilityEventHandler(
      dispatchPopulateAccessibilityEventHandler:
          EventHandler<DispatchPopulateAccessibilityEventEvent>?,
  ): T = legacyOrStyled(
      {
        super.dispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler)
      },
  ) {
    it +
        LegacyCommonPropStyleItem(
            LegacyCommonPropField.DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER,
            dispatchPopulateAccessibilityEventHandler,
        )
  }

  override fun visibilityOutputTag(visibilityOutputTag: String?): T =
      legacyOrStyled({ super.visibilityOutputTag(visibilityOutputTag) }) {
        it +
            LegacyCommonPropStyleItem(
                LegacyCommonPropField.VISIBILITY_OUTPUT_TAG,
                visibilityOutputTag,
            )
      }

  override fun addSystemGestureExclusionZone(exclusion: (Rect) -> Rect): T =
      legacyOrStyled({ super.addSystemGestureExclusionZone(exclusion) }) {
        it.addSystemGestureExclusionZone(exclusion)
      }

  // endregion

  override fun hasClickHandlerSet(): Boolean =
      if (buildKComponent) clickHandlerSet else super.hasClickHandlerSet()

  @Deprecated("Deprecated in Java")
  override fun hasBackgroundSet(): Boolean =
      if (buildKComponent) backgroundSet else super.hasBackgroundSet()

  override fun setComponent(component: Component) {
    // The backing component is fixed for the lifetime of this compatibility builder.
  }

  /** The accumulated style, folding in a pending transitionKey. */
  private fun finalizeStyle(): Style {
    transitionKeyValue?.let { key ->
      style =
          style.transitionKey(
              context,
              key,
              transitionKeyTypeValue ?: Transition.DEFAULT_TRANSITION_KEY_TYPE,
          )
      transitionKeyValue = null
    }
    return style
  }

  /** Applies the pending key/handle to the freshly-built [component]. */
  private fun finishBuild(component: Component): Component {
    keyValue?.let { setKeyForComponentInternal(component, it) }
    handleValue?.let { setHandleForComponentInternal(component, it) }
    return component
  }

  /** Whether [build] should return a KRow/KColumn instead of a legacy SpecGenerated Row/Column. */
  protected fun isKComponentEnabled(): Boolean = buildKComponent

  /** Flag-OFF path: finish the legacy component whose props were populated by the setters. */
  protected fun buildLegacy(): Component {
    val component = scratch
    applyContainerProps(component)
    return finishBuild(component)
  }

  /** Sets the container props ([alignContentValue] etc.) onto the concrete legacy [component]. */
  protected abstract fun applyContainerProps(component: SpecGeneratedComponent)

  protected fun buildKRow(): Component {
    val s = finalizeStyle()
    return finishBuild(
        KRow(
            alignContent = alignContentValue,
            alignItems = alignItemsValue,
            justifyContent = justifyContentValue,
            wrap = wrapValue,
            isReversed = reverseValue,
            gap = gapValue,
            children = children,
            style = s,
            customSimpleName = customSimpleName,
        ),
    )
  }

  protected fun buildKColumn(): Component {
    val s = finalizeStyle()
    return finishBuild(
        KColumn(
            alignContent = alignContentValue,
            alignItems = alignItemsValue,
            justifyContent = justifyContentValue,
            wrap = wrapValue,
            isReversed = reverseValue,
            gap = gapValue,
            children = children,
            style = s,
            customSimpleName = customSimpleName,
        ),
    )
  }
}

private enum class LegacyCommonPropField : StyleItemField {
  VISIBILITY_OUTPUT_TAG,
  DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER,
  STATE_LIST_ANIMATOR_RES,
  SHADOW_ELEVATION_PX,
}

private data class LegacyCommonPropStyleItem(
    override val field: LegacyCommonPropField,
    override val value: Any?,
) : StyleItem<Any?> {

  @Suppress("UNCHECKED_CAST")
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      LegacyCommonPropField.VISIBILITY_OUTPUT_TAG ->
          commonProps.visibilityOutputTag(value as String?)
      LegacyCommonPropField.DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER ->
          commonProps.dispatchPopulateAccessibilityEventHandler(
              value as EventHandler<DispatchPopulateAccessibilityEventEvent>?,
          )
      LegacyCommonPropField.STATE_LIST_ANIMATOR_RES ->
          commonProps.stateListAnimatorRes(
              value as? Int ?: error("STATE_LIST_ANIMATOR_RES requires an Int value"),
          )
      LegacyCommonPropField.SHADOW_ELEVATION_PX ->
          commonProps.shadowElevationPx(
              value as? Float ?: error("SHADOW_ELEVATION_PX requires a Float value"),
          )
    }
  }
}

/** Carries a raw legacy EventHandler onto CommonProps without changing its identity. */
private data class RawEventHandlerStyleItem(
    override val field: StyleItemField,
    override val value: EventHandler<*>?,
) : StyleItem<EventHandler<*>?> {

  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (val handlerField = field) {
      is ObjectField -> applyObjectHandler(handlerField, commonProps)
      is VisibilityField -> applyVisibilityHandler(handlerField, commonProps)
      is AccessibilityField -> applyAccessibilityHandler(handlerField, commonProps)
      else -> error("Unsupported raw EventHandler field: $handlerField")
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun applyObjectHandler(field: ObjectField, commonProps: CommonProps) {
    when (field) {
      ObjectField.ON_CLICK -> commonProps.clickHandler(value as EventHandler<ClickEvent>?)
      ObjectField.ON_LONG_CLICK ->
          commonProps.longClickHandler(value as EventHandler<LongClickEvent>?)
      ObjectField.ON_TOUCH -> commonProps.touchHandler(value as EventHandler<TouchEvent>?)
      ObjectField.ON_INTERCEPT_TOUCH ->
          commonProps.interceptTouchHandler(value as EventHandler<InterceptTouchEvent>?)
      ObjectField.ON_FOCUS_CHANGED ->
          commonProps.focusChangeHandler(value as EventHandler<FocusChangedEvent>?)
      else -> error("Unsupported raw ObjectField EventHandler: $field")
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun applyVisibilityHandler(field: VisibilityField, commonProps: CommonProps) {
    when (field) {
      VisibilityField.ON_VISIBLE ->
          commonProps.visibleHandler(
              value as EventHandler<com.facebook.rendercore.visibility.VisibleEvent>?,
          )
      VisibilityField.ON_INVISIBLE ->
          commonProps.invisibleHandler(
              value as EventHandler<com.facebook.rendercore.visibility.InvisibleEvent>?,
          )
      VisibilityField.ON_FOCUSED ->
          commonProps.focusedHandler(
              value as EventHandler<com.facebook.rendercore.visibility.FocusedVisibleEvent>?,
          )
      VisibilityField.ON_UNFOCUSED ->
          commonProps.unfocusedHandler(
              value as EventHandler<com.facebook.rendercore.visibility.UnfocusedVisibleEvent>?,
          )
      VisibilityField.ON_FULL_IMPRESSION ->
          commonProps.fullImpressionHandler(
              value as EventHandler<com.facebook.rendercore.visibility.FullImpressionVisibleEvent>?,
          )
      VisibilityField.ON_VISIBILITY_CHANGED ->
          commonProps.visibilityChangedHandler(
              value as EventHandler<com.facebook.rendercore.visibility.VisibilityChangedEvent>?,
          )
      else -> error("Unsupported raw VisibilityField EventHandler: $field")
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun applyAccessibilityHandler(field: AccessibilityField, commonProps: CommonProps) {
    when (field) {
      AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_NODE_INFO ->
          commonProps.onInitializeAccessibilityNodeInfoHandler(
              value as EventHandler<OnInitializeAccessibilityNodeInfoEvent>?,
          )
      AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_EVENT ->
          commonProps.onInitializeAccessibilityEventHandler(
              value as EventHandler<OnInitializeAccessibilityEventEvent>?,
          )
      AccessibilityField.ON_POPULATE_ACCESSIBILITY_EVENT ->
          commonProps.onPopulateAccessibilityEventHandler(
              value as EventHandler<OnPopulateAccessibilityEventEvent>?,
          )
      AccessibilityField.ON_REQUEST_SEND_ACCESSIBILITY_EVENT ->
          commonProps.onRequestSendAccessibilityEventHandler(
              value as EventHandler<OnRequestSendAccessibilityEventEvent>?,
          )
      AccessibilityField.PERFORM_ACCESSIBILITY_ACTION ->
          commonProps.performAccessibilityActionHandler(
              value as EventHandler<PerformAccessibilityActionEvent>?,
          )
      AccessibilityField.SEND_ACCESSIBILITY_EVENT ->
          commonProps.sendAccessibilityEventHandler(
              value as EventHandler<SendAccessibilityEventEvent>?,
          )
      AccessibilityField.SEND_ACCESSIBILITY_EVENT_UNCHECKED ->
          commonProps.sendAccessibilityEventUncheckedHandler(
              value as EventHandler<SendAccessibilityEventUncheckedEvent>?,
          )
      AccessibilityField.ON_PERFORM_ACTION_FOR_VIRTUAL_VIEW ->
          commonProps.onPerformActionForVirtualViewHandler(
              value as EventHandler<PerformActionForVirtualViewEvent>?,
          )
      AccessibilityField.ON_VIRTUAL_VIEW_KEYBOARD_FOCUS_CHANGED ->
          commonProps.onVirtualViewKeyboardFocusChangedHandler(
              value as EventHandler<VirtualViewKeyboardFocusChangedEvent>?,
          )
      else -> error("Unsupported raw AccessibilityField EventHandler: $field")
    }
  }
}
