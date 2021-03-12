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

import android.graphics.drawable.Drawable
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
private enum class ObjectField {
  BACKGROUND,
  FOREGROUND,
  ON_CLICK,
  ON_LONG_CLICK,
  WRAP_IN_VIEW,
  VIEW_TAG,
  VIEW_TAGS,
  OUTLINE_PROVIDER,
}

/** Enums for [FloatStyleItem]. */
private enum class FloatField {
  ALPHA,
  ELEVATION,
}

/** Common style item for all object styles. See note on [DimenField] about this pattern. */
private class ObjectStyleItem(val field: ObjectField, val value: Any?) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
      ObjectField.BACKGROUND -> commonProps.background(value as Drawable?)
      ObjectField.FOREGROUND -> commonProps.foreground(value as Drawable?)
      ObjectField.ON_CLICK ->
          commonProps.clickHandler(eventHandler(value as ((ClickEvent) -> Unit)))
      ObjectField.ON_LONG_CLICK ->
          commonProps.longClickHandler(
              eventHandlerWithReturn(value as ((LongClickEvent) -> Boolean)))
      ObjectField.WRAP_IN_VIEW -> commonProps.wrapInView()
      ObjectField.VIEW_TAG -> commonProps.viewTag(value)
      ObjectField.VIEW_TAGS -> commonProps.viewTags(value as SparseArray<Any>)
      ObjectField.OUTLINE_PROVIDER -> commonProps.outlineProvider(value as ViewOutlineProvider?)
    }.exhaustive
  }
}

/** Common style item for all float styles. See note on [FloatField] about this pattern. */
private class FloatStyleItem(val field: FloatField, val value: Float) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
      FloatField.ALPHA -> commonProps.alpha(value)
      FloatField.ELEVATION -> commonProps.shadowElevationPx(value)
    }.exhaustive
  }
}

/**
 * Sets a background on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setBackground]
 */
fun Style.background(background: Drawable?) =
    this + ObjectStyleItem(ObjectField.BACKGROUND, background)

/**
 * Sets ColorDrawable of the given color as a background on the View this Component mounts to.
 * Setting this property will cause the Component to be represented as a View at mount time if it
 * wasn't going to already.
 *
 * See [android.view.View.setBackgroundColor]
 */
fun Style.backgroundColor(@ColorInt backgroundColor: Int) =
    this + ObjectStyleItem(ObjectField.BACKGROUND, ComparableColorDrawable.create(backgroundColor))

/**
 * Sets a foreground on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setForeground]
 */
fun Style.foreground(foreground: Drawable?) =
    this + ObjectStyleItem(ObjectField.FOREGROUND, foreground)

/**
 * Sets a listener that will invoke the given lambda when this Component is clicked. Setting this
 * property will cause the Component to be represented as a View at mount time if it wasn't going to
 * already.
 */
fun Style.onClick(onClick: (ClickEvent) -> Unit) =
    this + ObjectStyleItem(ObjectField.ON_CLICK, onClick)

/**
 * Sets a listener that will invoke the given lambda when this Component is long clicked. Setting
 * this property will cause the Component to be represented as a View at mount time if it wasn't
 * going to already.
 */
fun Style.onLongClick(onLongClick: (LongClickEvent) -> Boolean) =
    this + ObjectStyleItem(ObjectField.ON_LONG_CLICK, onLongClick)

/**
 * Specifies that this Component should be represented as a View when this hierarchy is mounted.
 *
 * By default a Component does not mount any content. The exceptions to this are if:
 * - The Component renders to a [com.facebook.litho.annotations.MountSpec] with View mount content
 * - The Component has styles that require a View (like [background])
 * - The Component has the wrapInView style
 */
fun Style.wrapInView() = this + ObjectStyleItem(ObjectField.WRAP_IN_VIEW, null)

/**
 * Adds a View tag to the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setTag]
 */
fun Style.viewTag(viewTag: Any) = this + ObjectStyleItem(ObjectField.VIEW_TAG, viewTag)

/**
 * Adds a set of View tags to the View this Component mounts to. Setting this property will cause
 * the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * For each Int, Any pair in the given SparseArray, `View.setTag(Int, Any)` will be called. These
 * tags can be retrieved with `View.getTag(Int)`
 *
 * See [android.view.View.setTag]
 */
fun Style.viewTags(viewTags: SparseArray<out Any>) =
    this + ObjectStyleItem(ObjectField.VIEW_TAGS, viewTags)

/**
 * Sets an alpha on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setAlpha]
 */
fun Style.alpha(alpha: Float) = this + FloatStyleItem(FloatField.ALPHA, alpha)

/**
 * Sets an elevation on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * NOTE: This style will be ignored pre-API 21.
 *
 * See [android.view.View.setElevation]
 */
fun Style.elevation(elevation: Float) = this + FloatStyleItem(FloatField.ELEVATION, elevation)

/**
 * Sets a [ViewOutlineProvider] on the View this Component mounts to. Setting this property will
 * cause the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * NOTE: This style will be ignored pre-API 21.
 *
 * See [android.view.View.setOutlineProvider]
 */
fun Style.outlineProvider(outlineProvider: ViewOutlineProvider?) =
    this + ObjectStyleItem(ObjectField.OUTLINE_PROVIDER, outlineProvider)
