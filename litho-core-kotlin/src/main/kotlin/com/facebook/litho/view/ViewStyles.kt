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

package com.facebook.litho.view

import android.animation.StateListAnimator
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.SparseArray
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.LongClickEvent
import com.facebook.litho.ResourceResolver
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.litho.eventHandler
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.exhaustive
import com.facebook.litho.getCommonPropsHolder

/** Enums for [ObjectStyleItem]. */
@PublishedApi
internal enum class ObjectField {
  BACKGROUND,
  CLICKABLE,
  CLIP_CHILDREN,
  CLIP_TO_OUTLINE,
  FOCUSABLE,
  FOREGROUND,
  ON_CLICK,
  ON_LONG_CLICK,
  OUTLINE_PROVIDER,
  SELECTED,
  STATE_LIST_ANIMATOR,
  TEST_KEY,
  TRANSITION_NAME,
  WRAP_IN_VIEW,
  VIEW_TAG,
  VIEW_TAGS,
}

/** Enums for [FloatStyleItem]. */
@PublishedApi
internal enum class FloatField {
  ALPHA,
  ELEVATION,
  ROTATION,
  ROTATION_X,
  ROTATION_Y,
  SCALE,
}

/** Common style item for all object styles. See note on [DimenField] about this pattern. */
@PublishedApi
internal data class ObjectStyleItem(val field: ObjectField, val value: Any?) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
      ObjectField.BACKGROUND -> commonProps.background(value as Drawable?)
      ObjectField.CLICKABLE -> commonProps.clickable(value as Boolean)
      ObjectField.CLIP_CHILDREN -> commonProps.clipChildren(value as Boolean)
      ObjectField.CLIP_TO_OUTLINE -> commonProps.clipToOutline(value as Boolean)
      ObjectField.FOCUSABLE -> commonProps.focusable(value as Boolean)
      ObjectField.FOREGROUND -> commonProps.foreground(value as Drawable?)
      ObjectField.ON_CLICK ->
          commonProps.clickHandler(eventHandler(value as ((ClickEvent) -> Unit)))
      ObjectField.ON_LONG_CLICK ->
          commonProps.longClickHandler(
              eventHandlerWithReturn(value as ((LongClickEvent) -> Boolean)))
      ObjectField.SELECTED -> commonProps.selected(value as Boolean)
      ObjectField.STATE_LIST_ANIMATOR -> commonProps.stateListAnimator(value as StateListAnimator?)
      ObjectField.TEST_KEY -> commonProps.testKey(value as String?)
      ObjectField.TRANSITION_NAME -> commonProps.transitionName(value as String?)
      ObjectField.WRAP_IN_VIEW -> commonProps.wrapInView()
      ObjectField.VIEW_TAG -> commonProps.viewTag(value)
      ObjectField.VIEW_TAGS -> commonProps.viewTags(value as SparseArray<Any>)
      ObjectField.OUTLINE_PROVIDER -> commonProps.outlineProvider(value as ViewOutlineProvider?)
    }.exhaustive
  }
}

/** Common style item for all float styles. See note on [FloatField] about this pattern. */
@PublishedApi
internal data class FloatStyleItem(val field: FloatField, val value: Float) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
      FloatField.ALPHA -> commonProps.alpha(value)
      FloatField.ELEVATION -> commonProps.shadowElevationPx(value)
      FloatField.ROTATION -> commonProps.rotation(value)
      FloatField.ROTATION_X -> commonProps.rotationX(value)
      FloatField.ROTATION_Y -> commonProps.rotationY(value)
      FloatField.SCALE -> commonProps.scale(value)
    }.exhaustive
  }
}

/**
 * Sets an alpha on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setAlpha]
 */
inline fun Style.alpha(alpha: Float): Style = this + FloatStyleItem(FloatField.ALPHA, alpha)

/**
 * Sets a background on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setBackground]
 */
inline fun Style.background(background: Drawable?): Style =
    this + ObjectStyleItem(ObjectField.BACKGROUND, background)

/**
 * Sets ColorDrawable of the given color as a background on the View this Component mounts to.
 * Setting this property will cause the Component to be represented as a View at mount time if it
 * wasn't going to already.
 *
 * See [android.view.View.setBackgroundColor]
 */
inline fun Style.backgroundColor(@ColorInt backgroundColor: Int): Style =
    this + ObjectStyleItem(ObjectField.BACKGROUND, ComparableColorDrawable.create(backgroundColor))

/**
 * Sets if the View this Component mounts to should be clickable. Setting this property will cause
 * the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setClickable]
 */
inline fun Style.clickable(isClickable: Boolean): Style =
    this + ObjectStyleItem(ObjectField.CLICKABLE, isClickable)

/**
 * Setting this to false allows child views of this view to draw outside its bounds, overriding the
 * default behavior. It only applies to direct children. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 */
inline fun Style.clipChildren(clipChildren: Boolean): Style =
    this + ObjectStyleItem(ObjectField.CLIP_CHILDREN, clipChildren)

/**
 * Sets whether the View's Outline should be used to clip the contents of the View. Setting this
 * property will cause the Component to be represented as a View at mount time if it wasn't going to
 * already.
 *
 * See [android.view.View.setClipToOutline]
 */
inline fun Style.clipToOutline(clipToOutline: Boolean): Style =
    this + ObjectStyleItem(ObjectField.CLIP_TO_OUTLINE, clipToOutline)

/**
 * Sets an elevation on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * NOTE: This style will be ignored pre-API 21.
 *
 * See [android.view.View.setElevation]
 */
inline fun Style.elevation(elevation: Float): Style =
    this + FloatStyleItem(FloatField.ELEVATION, elevation)

/**
 * Setting this property will cause the Component to be focusable. Setting this property will cause
 * the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setFocusable]
 */
inline fun Style.focusable(isFocusable: Boolean): Style =
    this + ObjectStyleItem(ObjectField.FOCUSABLE, isFocusable)

/**
 * Sets a foreground on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setForeground]
 */
inline fun Style.foreground(foreground: Drawable?): Style =
    this + ObjectStyleItem(ObjectField.FOREGROUND, foreground)

/**
 * Sets a listener that will invoke the given lambda when this Component is clicked. Setting this
 * property will cause the Component to be represented as a View at mount time if it wasn't going to
 * already.
 */
inline fun Style.onClick(noinline onClick: (ClickEvent) -> Unit): Style =
    this + ObjectStyleItem(ObjectField.ON_CLICK, onClick)

/**
 * Sets a listener that will invoke the given lambda when this Component is long clicked. Setting
 * this property will cause the Component to be represented as a View at mount time if it wasn't
 * going to already.
 */
inline fun Style.onLongClick(noinline onLongClick: (LongClickEvent) -> Boolean): Style =
    this + ObjectStyleItem(ObjectField.ON_LONG_CLICK, onLongClick)

/**
 * Sets the degree that this component is rotated around the pivot point. Increasing the value
 * results in clockwise rotation. By default, the pivot point is centered on the component. Setting
 * this property will cause the Component to be represented as a View at mount time if it wasn't
 * going to already.
 *
 * See [android.view.View.setRotation]
 */
inline fun Style.rotation(rotation: Float): Style =
    this + FloatStyleItem(FloatField.ROTATION, rotation)

/**
 * Sets the degree that this component is rotated around the horizontal axis through the pivot
 * point. Setting this property will cause the Component to be represented as a View at mount time
 * if it wasn't going to already.
 *
 * See [android.view.View.setRotationX]
 */
inline fun Style.rotationX(rotationX: Float): Style =
    this + FloatStyleItem(FloatField.ROTATION_X, rotationX)

/**
 * Sets the degree that this component is rotated around the vertical axis through the pivot point.
 * Setting this property will cause the Component to be represented as a View at mount time if it
 * wasn't going to already.
 *
 * See [android.view.View.setRotationY]
 */
inline fun Style.rotationY(rotationY: Float): Style =
    this + FloatStyleItem(FloatField.ROTATION_Y, rotationY)

/**
 * Sets the scale (scaleX and scaleY) on this component. This is mostly relevant for animations and
 * being able to animate size changes. Otherwise for non-animation usecases, you should use the
 * standard layout properties to control the size of your component. Setting this property will
 * cause the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setScaleX] [android.view.View.setScaleY]
 */
inline fun Style.scale(scale: Float): Style = this + FloatStyleItem(FloatField.SCALE, scale)

/**
 * Changes the selection state of this Component. Setting this property will cause the Component to
 * be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setSelected]
 */
inline fun Style.selected(isSelected: Boolean): Style =
    this + ObjectStyleItem(ObjectField.SELECTED, isSelected)

/**
 * Attaches the provided StateListAnimator to this Component. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setStateListAnimator]
 *
 * NOTE: This style will be ignored pre-API 21.
 */
inline fun Style.stateListAnimator(stateListAnimator: StateListAnimator?): Style =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      this + ObjectStyleItem(ObjectField.STATE_LIST_ANIMATOR, stateListAnimator)
    } else {
      this
    }

/**
 * Sets testKey on the View this Component mounts to. Setting this property will cause the Component
 * to be represented as a View at mount time if it wasn't going to already.
 */
inline fun Style.testKey(testKey: String?): Style =
    this + ObjectStyleItem(ObjectField.TEST_KEY, testKey)

/**
 * Sets Activity transition name on the View this Component mounts to. Setting this property will
 * cause the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setTransitionName]
 */
inline fun Style.transitionName(transitionName: String?): Style =
    this + ObjectStyleItem(ObjectField.TRANSITION_NAME, transitionName)

/**
 * Specifies that this Component should be represented as a View when this hierarchy is mounted.
 *
 * By default a Component does not mount any content. The exceptions to this are if:
 * - The Component renders to a [com.facebook.litho.annotations.MountSpec] with View mount content
 * - The Component has styles that require a View (like [background])
 * - The Component has the wrapInView style
 */
inline fun Style.wrapInView(): Style = this + ObjectStyleItem(ObjectField.WRAP_IN_VIEW, null)

/**
 * Adds a View tag to the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setTag]
 */
inline fun Style.viewTag(viewTag: Any): Style =
    this + ObjectStyleItem(ObjectField.VIEW_TAG, viewTag)

/**
 * Adds a set of View tags to the View this Component mounts to. Setting this property will cause
 * the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * For each Int, Any pair in the given SparseArray, `View.setTag(Int, Any)` will be called. These
 * tags can be retrieved with `View.getTag(Int)`
 *
 * See [android.view.View.setTag]
 */
inline fun Style.viewTags(viewTags: SparseArray<out Any>): Style =
    this + ObjectStyleItem(ObjectField.VIEW_TAGS, viewTags)

/**
 * Sets a [ViewOutlineProvider] on the View this Component mounts to. Setting this property will
 * cause the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * NOTE: This style will be ignored pre-API 21.
 *
 * See [android.view.View.setOutlineProvider]
 */
inline fun Style.outlineProvider(outlineProvider: ViewOutlineProvider?): Style =
    this + ObjectStyleItem(ObjectField.OUTLINE_PROVIDER, outlineProvider)
