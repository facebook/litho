/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import android.graphics.drawable.Drawable
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

/**
 * Enums for [DimenStyleItem]. In the longer term, the vision is to have these style items
 * decentralized, but since we have other blockers to that first, we are using enums to limit the
 * number of style item types we need to create.
 */
private enum class DimenField {
  WIDTH,
  HEIGHT,
  MIN_WIDTH,
  MAX_WIDTH,
  MIN_HEIGHT,
  MAX_HEIGHT,
  FLEX_BASIS,
  PADDING_START,
  PADDING_TOP,
  PADDING_END,
  PADDING_BOTTOM,
  PADDING_HORIZONTAL,
  PADDING_VERTICAL,
  PADDING_ALL,
  MARGIN_START,
  MARGIN_TOP,
  MARGIN_END,
  MARGIN_BOTTOM,
  MARGIN_HORIZONTAL,
  MARGIN_VERTICAL,
  MARGIN_ALL,
  POSITION_START,
  POSITION_TOP,
  POSITION_END,
  POSITION_BOTTOM,
}

/** Enums for [FloatStyleItem]. */
private enum class FloatField {
  WIDTH_PERCENT,
  HEIGHT_PERCENT,
  MIN_WIDTH_PERCENT,
  MAX_WIDTH_PERCENT,
  MIN_HEIGHT_PERCENT,
  MAX_HEIGHT_PERCENT,
  FLEX,
  FLEX_GROW,
  FLEX_SHRINK,
  ASPECT_RATIO,
}

/** Enums for [ObjectStyleItem]. */
private enum class ObjectField {
  ALIGN_SELF,
  POSITION_TYPE,
  BACKGROUND,
  FOREGROUND,
  ON_CLICK,
  ON_LONG_CLICK,
  ON_VISIBLE,
  ON_FOCUSED,
  ON_FULL_IMPRESSION,
  WRAP_IN_VIEW,
  VIEW_TAG,
}

/**
 * Part of a [Style] that can apply an attribute to an underlying Component, e.g. width or click
 * handling.
 */
interface StyleItem {

  /** Sets this style item value on the given [CommonProps]. */
  fun DslScope.applyToProps(commonProps: CommonProps)
}

/** Common style item for all dimen styles. See note on [DimenField] about this pattern. */
private class DimenStyleItem(val field: DimenField, val value: Dimen) : StyleItem {
  override fun DslScope.applyToProps(commonProps: CommonProps) {
    val pixelValue = value.toPixels()
    when (field) {
      DimenField.WIDTH -> commonProps.widthPx(if (value == Dimen.Hairline) 1 else pixelValue)
      DimenField.HEIGHT -> commonProps.heightPx(if (value == Dimen.Hairline) 1 else pixelValue)
      DimenField.MIN_WIDTH -> commonProps.minWidthPx(pixelValue)
      DimenField.MAX_WIDTH -> commonProps.maxWidthPx(pixelValue)
      DimenField.MIN_HEIGHT -> commonProps.minHeightPx(pixelValue)
      DimenField.MAX_HEIGHT -> commonProps.maxHeightPx(pixelValue)
      DimenField.FLEX_BASIS -> commonProps.flexBasisPx(pixelValue)
      DimenField.PADDING_START -> commonProps.paddingPx(YogaEdge.START, pixelValue)
      DimenField.PADDING_TOP -> commonProps.paddingPx(YogaEdge.TOP, pixelValue)
      DimenField.PADDING_END -> commonProps.paddingPx(YogaEdge.END, pixelValue)
      DimenField.PADDING_BOTTOM -> commonProps.paddingPx(YogaEdge.BOTTOM, pixelValue)
      DimenField.PADDING_HORIZONTAL -> commonProps.paddingPx(YogaEdge.HORIZONTAL, pixelValue)
      DimenField.PADDING_VERTICAL -> commonProps.paddingPx(YogaEdge.VERTICAL, pixelValue)
      DimenField.PADDING_ALL -> commonProps.paddingPx(YogaEdge.ALL, pixelValue)
      DimenField.MARGIN_START -> commonProps.marginPx(YogaEdge.START, pixelValue)
      DimenField.MARGIN_TOP -> commonProps.marginPx(YogaEdge.TOP, pixelValue)
      DimenField.MARGIN_END -> commonProps.marginPx(YogaEdge.END, pixelValue)
      DimenField.MARGIN_BOTTOM -> commonProps.marginPx(YogaEdge.BOTTOM, pixelValue)
      DimenField.MARGIN_HORIZONTAL -> commonProps.marginPx(YogaEdge.HORIZONTAL, pixelValue)
      DimenField.MARGIN_VERTICAL -> commonProps.marginPx(YogaEdge.VERTICAL, pixelValue)
      DimenField.MARGIN_ALL -> commonProps.marginPx(YogaEdge.ALL, pixelValue)
      DimenField.POSITION_START -> commonProps.positionPx(YogaEdge.START, pixelValue)
      DimenField.POSITION_END -> commonProps.positionPx(YogaEdge.END, pixelValue)
      DimenField.POSITION_TOP -> commonProps.positionPx(YogaEdge.TOP, pixelValue)
      DimenField.POSITION_BOTTOM -> commonProps.positionPx(YogaEdge.BOTTOM, pixelValue)
    }.exhaustive
  }
}

/** Common style item for all float styles. See note on [DimenField] about this pattern. */
private class FloatStyleItem(val field: FloatField, val value: Float) : StyleItem {
  override fun DslScope.applyToProps(commonProps: CommonProps) {
    when (field) {
      FloatField.WIDTH_PERCENT -> commonProps.widthPercent(value)
      FloatField.HEIGHT_PERCENT -> commonProps.heightPercent(value)
      FloatField.MIN_WIDTH_PERCENT -> commonProps.minWidthPercent(value)
      FloatField.MAX_WIDTH_PERCENT -> commonProps.maxWidthPercent(value)
      FloatField.MIN_HEIGHT_PERCENT -> commonProps.minHeightPercent(value)
      FloatField.MAX_HEIGHT_PERCENT -> commonProps.maxHeightPercent(value)
      FloatField.FLEX -> commonProps.flex(value)
      FloatField.FLEX_GROW -> commonProps.flexGrow(value)
      FloatField.FLEX_SHRINK -> commonProps.flexShrink(value)
      FloatField.ASPECT_RATIO -> commonProps.aspectRatio(value)
    }.exhaustive
  }
}

/** Common style item for all object styles. See note on [DimenField] about this pattern. */
private class ObjectStyleItem(val field: ObjectField, val value: Any?) : StyleItem {
  override fun DslScope.applyToProps(commonProps: CommonProps) {
    when (field) {
      ObjectField.ALIGN_SELF -> value?.let { commonProps.alignSelf(it as YogaAlign) }
      ObjectField.POSITION_TYPE -> value?.let { commonProps.positionType(it as YogaPositionType) }
      ObjectField.BACKGROUND -> commonProps.background(value as Drawable?)
      ObjectField.FOREGROUND -> commonProps.foreground(value as Drawable?)
      ObjectField.ON_CLICK ->
          commonProps.clickHandler(eventHandler(value as ((ClickEvent) -> Unit)))
      ObjectField.ON_LONG_CLICK ->
          commonProps.longClickHandler(
              eventHandlerWithReturn(value as ((LongClickEvent) -> Boolean)))
      ObjectField.ON_VISIBLE ->
          commonProps.visibleHandler(eventHandler(value as (VisibleEvent) -> Unit))
      ObjectField.ON_FOCUSED ->
          commonProps.focusedHandler(eventHandler(value as (FocusedVisibleEvent) -> Unit))
      ObjectField.ON_FULL_IMPRESSION ->
          commonProps.fullImpressionHandler(
              eventHandler(value as (FullImpressionVisibleEvent) -> Unit))
      ObjectField.WRAP_IN_VIEW -> commonProps.wrapInView()
      ObjectField.VIEW_TAG -> commonProps.viewTag(value)
    }.exhaustive
  }
}

/**
 * Tiny trick to make the compiler think a when result is used and enforce exhaustiveness of cases.
 */
private val Any?.exhaustive: Unit
  get() = Unit

/**
 * An immutable ordered collection of attributes ( [StyleItem] s) that can be applied to a
 * component, like width or click handling.
 *
 * Adding new attributes to a Style (e.g. by calling `.background`) will return a new Style object.
 * Ordering matters in that the last definition of an attribute 'wins'.
 *
 * Styles can also be added via the `+` operator, with attributes from the right-hand side Style
 * taking precedence if the two define different values for the same attribute, similar to adding
 * maps.
 */
open class Style(
    /**
     * This is the Style we're adding to, e.g. `Style.padding()` when calling `.background()` in
     * `Style.padding().background()`
     */
    private val previousStyle: Style?,

    /**
     * This is the [StyleItem] we're adding, e.g. the background when calling `.background()` in
     * `Style.padding().background()`
     */
    private val item: StyleItem?,
) {

  operator fun plus(other: Style?): Style {
    if (other == null) {
      return this
    }
    return CombinedStyle(if (this == Style) null else this, other)
  }

  inline operator fun plus(nextItem: StyleItem?): Style {
    if (nextItem == null) {
      return this
    }
    return Style(if (this == Style) null else this, nextItem)
  }

  fun size(size: Dimen) =
      this + DimenStyleItem(DimenField.WIDTH, size) + DimenStyleItem(DimenField.HEIGHT, size)

  fun size(width: Dimen? = null, height: Dimen? = null) =
      this +
          width?.let { DimenStyleItem(DimenField.WIDTH, it) } +
          height?.let { DimenStyleItem(DimenField.HEIGHT, it) }

  fun width(minWidth: Dimen? = null, maxWidth: Dimen? = null) =
      this +
          minWidth?.let { DimenStyleItem(DimenField.MIN_WIDTH, it) } +
          maxWidth?.let { DimenStyleItem(DimenField.MAX_WIDTH, it) }

  fun height(minHeight: Dimen? = null, maxHeight: Dimen? = null) =
      this +
          minHeight?.let { DimenStyleItem(DimenField.MIN_HEIGHT, it) } +
          maxHeight?.let { DimenStyleItem(DimenField.MAX_HEIGHT, it) }

  fun flex(grow: Float? = null, shrink: Float? = null, basis: Dimen? = null) =
      this +
          grow?.let { FloatStyleItem(FloatField.FLEX_GROW, it) } +
          shrink?.let { FloatStyleItem(FloatField.FLEX_SHRINK, it) } +
          basis?.let { DimenStyleItem(DimenField.FLEX_BASIS, it) }

  fun alignSelf(align: YogaAlign) = this + ObjectStyleItem(ObjectField.ALIGN_SELF, align)

  fun aspectRatio(aspectRatio: Float) = this + FloatStyleItem(FloatField.ASPECT_RATIO, aspectRatio)

  fun padding(all: Dimen) = this + DimenStyleItem(DimenField.PADDING_ALL, all)

  fun padding(
      all: Dimen? = null,
      horizontal: Dimen? = null,
      vertical: Dimen? = null,
      start: Dimen? = null,
      top: Dimen? = null,
      end: Dimen? = null,
      bottom: Dimen? = null
  ) =
      this +
          all?.let { DimenStyleItem(DimenField.PADDING_ALL, it) } +
          horizontal?.let { DimenStyleItem(DimenField.PADDING_HORIZONTAL, it) } +
          vertical?.let { DimenStyleItem(DimenField.PADDING_VERTICAL, it) } +
          start?.let { DimenStyleItem(DimenField.PADDING_START, it) } +
          top?.let { DimenStyleItem(DimenField.PADDING_TOP, it) } +
          end?.let { DimenStyleItem(DimenField.PADDING_END, it) } +
          bottom?.let { DimenStyleItem(DimenField.PADDING_BOTTOM, it) }

  fun margin(all: Dimen) = this + DimenStyleItem(DimenField.MARGIN_ALL, all)

  fun margin(
      all: Dimen? = null,
      horizontal: Dimen? = null,
      vertical: Dimen? = null,
      start: Dimen? = null,
      top: Dimen? = null,
      end: Dimen? = null,
      bottom: Dimen? = null
  ) =
      this +
          all?.let { DimenStyleItem(DimenField.MARGIN_ALL, it) } +
          horizontal?.let { DimenStyleItem(DimenField.MARGIN_HORIZONTAL, it) } +
          vertical?.let { DimenStyleItem(DimenField.MARGIN_VERTICAL, it) } +
          start?.let { DimenStyleItem(DimenField.MARGIN_START, it) } +
          top?.let { DimenStyleItem(DimenField.MARGIN_TOP, it) } +
          end?.let { DimenStyleItem(DimenField.MARGIN_END, it) } +
          bottom?.let { DimenStyleItem(DimenField.MARGIN_BOTTOM, it) }

  fun position(
      start: Dimen? = null,
      top: Dimen? = null,
      end: Dimen? = null,
      bottom: Dimen? = null
  ) =
      this +
          start?.let { DimenStyleItem(DimenField.POSITION_START, it) } +
          top?.let { DimenStyleItem(DimenField.POSITION_TOP, it) } +
          end?.let { DimenStyleItem(DimenField.POSITION_END, it) } +
          bottom?.let { DimenStyleItem(DimenField.POSITION_BOTTOM, it) }

  fun positionType(positionType: YogaPositionType) =
      this + ObjectStyleItem(ObjectField.POSITION_TYPE, positionType)

  fun background(background: Drawable?) = this + ObjectStyleItem(ObjectField.BACKGROUND, background)

  fun foreground(foreground: Drawable?) = this + ObjectStyleItem(ObjectField.FOREGROUND, foreground)

  fun onClick(onClick: (ClickEvent) -> Unit) = this + ObjectStyleItem(ObjectField.ON_CLICK, onClick)

  fun onLongClick(onLongClick: (LongClickEvent) -> Boolean) =
      this + ObjectStyleItem(ObjectField.ON_LONG_CLICK, onLongClick)

  fun onVisible(onVisible: (VisibleEvent) -> Unit) =
      this + ObjectStyleItem(ObjectField.ON_VISIBLE, onVisible)

  fun onFocusedVisible(onFocused: (FocusedVisibleEvent) -> Unit) =
      this + ObjectStyleItem(ObjectField.ON_FOCUSED, onFocused)

  fun onFullImpression(onFullImpression: (FullImpressionVisibleEvent) -> Unit) =
      this + ObjectStyleItem(ObjectField.ON_FULL_IMPRESSION, onFullImpression)

  fun wrapInView() = this + ObjectStyleItem(ObjectField.WRAP_IN_VIEW, null)

  fun viewTag(viewTag: Any) = this + ObjectStyleItem(ObjectField.VIEW_TAG, viewTag)

  open fun forEach(lambda: (StyleItem) -> Unit) {
    previousStyle?.forEach(lambda)
    if (item != null) {
      lambda(item)
    }
  }

  /**
   * An empty Style singleton that can be used to build a chain of style items.
   *
   * This is a bit of a trick that lets us make `Style.background()` look like a static call, but
   * actually be a member call since `Style` is now a singleton object. Otherwise we'd need to
   * define both a static `Style.background()` and a member function to support
   * `Style.padding(...).background()`.
   */
  companion object : Style(null, null)
}

/**
 * A subclass of [Style] which combines two Styles, as opposed to adding a single [StyleItem] to an
 * existing Style.
 *
 * A CombinedStyle is created by combining two Styles with `+`. Attributes from the right-hand side
 * Style take precedence if the two define different values for the same attribute, similar to
 * adding maps.
 */
private class CombinedStyle(val first: Style?, val second: Style?) : Style(first, null) {

  override fun forEach(lambda: (StyleItem) -> Unit) {
    first?.forEach(lambda)
    second?.forEach(lambda)
  }
}

internal fun DslScope.copyStyleToProps(style: Style, props: CommonProps) {
  style.forEach { stylePiece -> with(stylePiece) { applyToProps(props) } }
}
