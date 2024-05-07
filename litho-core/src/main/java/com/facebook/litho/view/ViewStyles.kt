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

package com.facebook.litho.view

import android.animation.StateListAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.ClickEvent
import com.facebook.litho.CommonProps
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.FocusChangedEvent
import com.facebook.litho.InterceptTouchEvent
import com.facebook.litho.LongClickEvent
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.StyleItemField
import com.facebook.litho.SupportsPivotTransform
import com.facebook.litho.TouchEvent
import com.facebook.litho.binders.viewBinder
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.litho.eventHandler
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.RenderUnit
import com.facebook.yoga.YogaEdge

/** Enums for [ObjectStyleItem]. */
@PublishedApi
internal enum class ObjectField : StyleItemField {
  BACKGROUND,
  CLICKABLE,
  CLIP_CHILDREN,
  CLIP_TO_OUTLINE,
  DUPLICATE_CHILDREN_STATES,
  DUPLICATE_PARENT_STATE,
  FOCUSABLE,
  FOREGROUND,
  ON_CLICK,
  ON_FOCUS_CHANGED,
  ON_INTERCEPT_TOUCH,
  ON_LONG_CLICK,
  ON_TOUCH,
  OUTLINE_PROVIDER,
  SELECTED,
  STATE_LIST_ANIMATOR,
  TEST_KEY,
  TRANSITION_NAME,
  WRAP_IN_VIEW,
  VIEW_ID,
  VIEW_TAG,
  VIEW_TAGS,
  ENABLED,
  KEYBOARD_NAVIGATION_CLUSTER,
  ADD_TOUCH_EXCLUSION_ZONE,
  TOOLTIP_TEXT,
}

/** Enums for [FloatStyleItem]. */
@PublishedApi
internal enum class FloatField : StyleItemField {
  ALPHA,
  ROTATION,
  ROTATION_X,
  ROTATION_Y,
  SCALE,
}

/** Enums for [FloatStyleItem]. */
@PublishedApi
internal enum class DimenField : StyleItemField {
  TOUCH_EXPANSION_START,
  TOUCH_EXPANSION_TOP,
  TOUCH_EXPANSION_END,
  TOUCH_EXPANSION_BOTTOM,
  TOUCH_EXPANSION_LEFT,
  TOUCH_EXPANSION_RIGHT,
  TOUCH_EXPANSION_HORIZONTAL,
  TOUCH_EXPANSION_VERTICAL,
  TOUCH_EXPANSION_ALL,
  ELEVATION,
}

/** Common style item for all object styles. See note on [DimenField] about this pattern. */
@PublishedApi
@DataClassGenerate
internal data class ObjectStyleItem(override val field: ObjectField, override val value: Any?) :
    StyleItem<Any?> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      ObjectField.BACKGROUND -> commonProps.background(value as Drawable?)
      ObjectField.CLICKABLE -> commonProps.clickable(value as Boolean)
      ObjectField.CLIP_CHILDREN -> commonProps.clipChildren(value as Boolean)
      ObjectField.CLIP_TO_OUTLINE -> commonProps.clipToOutline(value as Boolean)
      ObjectField.DUPLICATE_CHILDREN_STATES -> commonProps.duplicateChildrenStates(value as Boolean)
      ObjectField.DUPLICATE_PARENT_STATE -> commonProps.duplicateParentState(value as Boolean)
      ObjectField.FOCUSABLE -> commonProps.focusable(value as Boolean)
      ObjectField.FOREGROUND -> commonProps.foreground(value as Drawable?)
      ObjectField.ON_FOCUS_CHANGED -> {
        val focusChangedHandler =
            if (value != null) eventHandler(value as (FocusChangedEvent) -> Unit) else null
        commonProps.focusChangeHandler(focusChangedHandler)
      }
      ObjectField.ON_CLICK -> {
        val clickHandler = if (value != null) eventHandler(value as (ClickEvent) -> Unit) else null
        commonProps.clickHandler(clickHandler)
      }
      ObjectField.ON_LONG_CLICK -> {
        val longClickHandler =
            if (value != null) eventHandlerWithReturn(value as ((LongClickEvent) -> Boolean))
            else null
        commonProps.longClickHandler(longClickHandler)
      }
      ObjectField.ON_INTERCEPT_TOUCH -> {
        val interceptTouchHandler =
            if (value != null) eventHandlerWithReturn(value as ((InterceptTouchEvent) -> Boolean))
            else null
        commonProps.interceptTouchHandler(interceptTouchHandler)
      }
      ObjectField.ON_TOUCH -> {
        val touchHandler =
            if (value != null) eventHandler(value as ((TouchEvent) -> Unit)) else null
        commonProps.touchHandler(touchHandler)
      }
      ObjectField.SELECTED -> commonProps.selected(value as Boolean)
      ObjectField.STATE_LIST_ANIMATOR -> commonProps.stateListAnimator(value as StateListAnimator?)
      ObjectField.TEST_KEY -> commonProps.testKey(value as String?)
      ObjectField.TRANSITION_NAME -> commonProps.transitionName(value as String?)
      ObjectField.WRAP_IN_VIEW -> commonProps.wrapInView()
      ObjectField.VIEW_ID -> commonProps.viewId(value as Int)
      ObjectField.VIEW_TAG -> commonProps.viewTag(value)
      ObjectField.VIEW_TAGS -> commonProps.viewTags(value as SparseArray<Any>)
      ObjectField.OUTLINE_PROVIDER -> commonProps.outlineProvider(value as ViewOutlineProvider?)
      ObjectField.ENABLED -> commonProps.enabled(value as Boolean)
      ObjectField.KEYBOARD_NAVIGATION_CLUSTER ->
          commonProps.keyboardNavigationCluster(value as Boolean)
      ObjectField.ADD_TOUCH_EXCLUSION_ZONE ->
          commonProps.addSystemGestureExclusionZone(value as (Rect) -> Rect)
      ObjectField.TOOLTIP_TEXT -> commonProps.tooltipText(value as String?)
    }
  }
}

/** Common style item for all float styles. See note on [FloatField] about this pattern. */
@PublishedApi
@DataClassGenerate
internal data class FloatStyleItem(override val field: FloatField, override val value: Float) :
    StyleItem<Float> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      FloatField.ALPHA -> commonProps.alpha(value)
      FloatField.ROTATION -> commonProps.rotation(value)
      FloatField.ROTATION_X -> commonProps.rotationX(value)
      FloatField.ROTATION_Y -> commonProps.rotationY(value)
      FloatField.SCALE -> commonProps.scale(value)
    }
  }
}

/** Common style item for all float styles. See note on [FloatField] about this pattern. */
@PublishedApi
@DataClassGenerate
internal data class DimenStyleItem(override val field: DimenField, override val value: Dimen) :
    StyleItem<Dimen> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    val pixelValue = value.toPixels(context.resourceResolver)
    when (field) {
      DimenField.TOUCH_EXPANSION_START -> commonProps.touchExpansionPx(YogaEdge.START, pixelValue)
      DimenField.TOUCH_EXPANSION_TOP -> commonProps.touchExpansionPx(YogaEdge.TOP, pixelValue)
      DimenField.TOUCH_EXPANSION_END -> commonProps.touchExpansionPx(YogaEdge.END, pixelValue)
      DimenField.TOUCH_EXPANSION_BOTTOM -> commonProps.touchExpansionPx(YogaEdge.BOTTOM, pixelValue)
      DimenField.TOUCH_EXPANSION_LEFT -> commonProps.touchExpansionPx(YogaEdge.LEFT, pixelValue)
      DimenField.TOUCH_EXPANSION_RIGHT -> commonProps.touchExpansionPx(YogaEdge.RIGHT, pixelValue)
      DimenField.TOUCH_EXPANSION_HORIZONTAL ->
          commonProps.touchExpansionPx(YogaEdge.HORIZONTAL, pixelValue)
      DimenField.TOUCH_EXPANSION_VERTICAL ->
          commonProps.touchExpansionPx(YogaEdge.VERTICAL, pixelValue)
      DimenField.TOUCH_EXPANSION_ALL -> commonProps.touchExpansionPx(YogaEdge.ALL, pixelValue)
      DimenField.ELEVATION -> commonProps.shadowElevationPx(pixelValue.toFloat())
    }
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

inline fun Style.backgroundColor(@ColorInt backgroundColor: Long): Style =
    backgroundColor(backgroundColor.toInt())

/**
 * Sets if the View this Component mounts to should be clickable. Setting this property will cause
 * the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setClickable]
 */
inline fun Style.clickable(isClickable: Boolean): Style =
    this + ObjectStyleItem(ObjectField.CLICKABLE, isClickable)

/**
 * Sets if the View this Component mounts to should be enabled. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setClickable]
 */
inline fun Style.enabled(isEnabled: Boolean): Style =
    this + ObjectStyleItem(ObjectField.ENABLED, isEnabled)

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
 * Sets whether this ViewGroup's drawable states also include its children's drawable states. This
 * is used, for example, to make a group appear to be focused when its child EditText or button is
 * focused. Setting this property will cause the Component to be represented as a View at mount time
 * if it wasn't going to already.
 *
 * See [android.view.ViewGroup.setAddStatesFromChildren]
 */
inline fun Style.duplicateChildrenStates(duplicateChildrenStates: Boolean): Style =
    this + ObjectStyleItem(ObjectField.DUPLICATE_CHILDREN_STATES, duplicateChildrenStates)

/**
 * Sets whether the View gets its drawable state (focused, pressed, etc.) from its direct parent
 * rather than from itself. Setting this property will cause the Component to be represented as a
 * View at mount time if it wasn't going to already.
 *
 * See [android.view.View.setDuplicateParentStateEnabled]
 */
inline fun Style.duplicateParentState(duplicateParentState: Boolean): Style =
    this + ObjectStyleItem(ObjectField.DUPLICATE_PARENT_STATE, duplicateParentState)

/**
 * Sets an elevation on the View this Component mounts to. Setting this property will cause the
 * Component to be represented as a View at mount time if it wasn't going to already.
 *
 * NOTE: This style will be ignored pre-API 21.
 *
 * See [android.view.View.setElevation]
 */
inline fun Style.elevation(elevation: Dimen): Style =
    this + DimenStyleItem(DimenField.ELEVATION, elevation)

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
 * Sets ColorDrawable of the given color as a background on the View this Component mounts to.
 * Setting this property will cause the Component to be represented as a View at mount time if it
 * wasn't going to already.
 *
 * See [android.view.View.setForeground]
 */
inline fun Style.foregroundColor(@ColorInt foregroundColor: Int): Style =
    this + ObjectStyleItem(ObjectField.FOREGROUND, ComparableColorDrawable.create(foregroundColor))

/**
 * Sets a listener that will invoke the given lambda when this Component's focus changes. Setting
 * this property will cause the Component to be represented as a View at mount time if it wasn't
 * going to already.
 *
 * See [android.view.View.OnFocusChangeListener]
 */
inline fun Style.onFocusedChanged(noinline action: (FocusChangedEvent) -> Unit): Style =
    this + ObjectStyleItem(ObjectField.ON_FOCUS_CHANGED, action)

/**
 * Sets a listener that will invoke the given lambda when this Component's focus changes but only if
 * [enabled] is true. If enabled, setting this property will cause the Component to be represented
 * as a View at mount time if it wasn't going to already.
 *
 * See [android.view.View.OnFocusChangeListener]
 */
inline fun Style.onFocusedChanged(
    enabled: Boolean,
    noinline action: (FocusChangedEvent) -> Unit
): Style = this + ObjectStyleItem(ObjectField.ON_FOCUS_CHANGED, if (enabled) action else null)

/**
 * Sets a listener that will invoke the given lambda when this Component is clicked. Setting this
 * property will cause the Component to be represented as a View at mount time if it wasn't going to
 * already.
 */
inline fun Style.onClick(noinline action: (ClickEvent) -> Unit): Style =
    this + ObjectStyleItem(ObjectField.ON_CLICK, action)

/**
 * Sets a listener that will invoke the given lambda when this Component is clicked but only if
 * [enabled] is true. If enabled, setting this property will cause the Component to be represented
 * as a View at mount time if it wasn't going to already.
 */
inline fun Style.onClick(enabled: Boolean, noinline action: (ClickEvent) -> Unit): Style =
    this + ObjectStyleItem(ObjectField.ON_CLICK, if (enabled) action else null)

/**
 * Sets a listener that will invoke the given lambda when this Component is long clicked. Setting
 * this property will cause the Component to be represented as a View at mount time if it wasn't
 * going to already.
 */
inline fun Style.onLongClick(noinline action: (LongClickEvent) -> Boolean): Style =
    this + ObjectStyleItem(ObjectField.ON_LONG_CLICK, action)

/**
 * Sets a listener that will invoke the given lambda when this Component is long clicked but only if
 * [enabled] is true. If enabled, setting this property will cause the Component to be represented
 * as a View at mount time if it wasn't going to already.
 */
inline fun Style.onLongClick(
    enabled: Boolean,
    noinline action: (LongClickEvent) -> Boolean
): Style = this + ObjectStyleItem(ObjectField.ON_LONG_CLICK, if (enabled) action else null)

/**
 * Sets a listener that will invoke the given lambda when this Component is touched. Setting this
 * property will cause the Component to be represented as a View at mount time if it wasn't going to
 * already.
 */
inline fun Style.onTouch(noinline action: (TouchEvent) -> Boolean): Style =
    this + ObjectStyleItem(ObjectField.ON_TOUCH, action)

/**
 * Sets a listener that will invoke the given lambda when this Component is touched but only if
 * [enabled] is true. If enabled, setting this property will cause the Component to be represented
 * as a View at mount time if it wasn't going to already.
 */
inline fun Style.onTouch(enabled: Boolean, noinline action: (TouchEvent) -> Boolean): Style =
    this + ObjectStyleItem(ObjectField.ON_TOUCH, if (enabled) action else null)

/**
 * Sets a listener that will intercept all touch screen motion events. This allows you to watch
 * events as they are dispatched to your children, and take ownership of the current gesture at any
 * point. Implementations should return true if they intercepted the event and wish to receive
 * subsequent events, and false otherwise. Setting this property will cause the Component to be
 * represented as a View at mount time if it wasn't going to already.
 *
 * See [android.view.ViewGroup.onInterceptTouchEvent]
 */
inline fun Style.onInterceptTouch(noinline action: (InterceptTouchEvent) -> Boolean): Style =
    this + ObjectStyleItem(ObjectField.ON_INTERCEPT_TOUCH, action)

/**
 * Sets a listener that will intercept all touch screen motion events but only if [enabled] is true.
 * This allows you to watch events as they are dispatched to your children, and take ownership of
 * the current gesture at any point. Implementations should return true if they intercepted the
 * event and wish to receive subsequent events, and false otherwise. If [enabled] is true, setting
 * this property will cause the Component to be represented as a View at mount time if it wasn't
 * going to already.
 *
 * See [android.view.ViewGroup.onInterceptTouchEvent]
 */
inline fun Style.onInterceptTouch(
    enabled: Boolean,
    noinline action: (InterceptTouchEvent) -> Boolean
): Style = this + ObjectStyleItem(ObjectField.ON_INTERCEPT_TOUCH, if (enabled) action else null)

/**
 * Sets the transform pivot of a View (used for scale and rotation transforms) to be centered at the
 * given percentages of the View's width and height. The default pivot point is (50f, 50f).
 *
 * The Component this Style is applied to must render to a View which implements
 * [SupportsPivotTransform]. Rows and Columns both render to ComponentHost which implements this
 * interface. If you need to apply a pivot to a Component that doesn't render to a View that
 * implements this interface, you can either implement this interface in the View it does render to,
 * or wrap this Component in a Row or Column and apply the transform and pivot there instead.
 *
 * Note: Unlike [View.setPivotX] and [View.setPivotY], the value of this pivot is a percentage, not
 * an absolute pixel value.
 *
 * @param pivotXPercent the percentage of the width to use as the pivotX
 * @param pivotYPercent the percentage of the height to use as the pivotY
 * @see SupportsPivotTransform
 * @see android.view.View.setPivotX
 * @see android.view.View.setPivotY
 */
inline fun Style.pivotPercent(
    @FloatRange(0.0, 100.0) pivotXPercent: Float = 50f,
    @FloatRange(0.0, 100.0) pivotYPercent: Float = 50f
): Style {
  check(pivotXPercent in 0f..100f && pivotYPercent in 0f..100f) {
    "Pivot values must be between 0 and 100f. Got ($pivotXPercent, $pivotYPercent)."
  }
  return this + Style.viewBinder(PivotBinder, Pair(pivotXPercent, pivotYPercent))
}

@PublishedApi
internal object PivotBinder : RenderUnit.Binder<Pair<Float, Float>, View, Unit> {

  private const val BadPivotClassErrorMessage =
      "Setting transform pivot is only supported on Views that implement SupportsPivotTransform. " +
          "If it isn't possible to add this interface to the View in question, wrap this " +
          "Component in a Row or Column and apply the transform and pivot there instead."

  override fun shouldUpdate(
      currentModel: Pair<Float, Float>,
      newModel: Pair<Float, Float>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean = currentModel != newModel

  override fun bind(context: Context, content: View, model: Pair<Float, Float>, layoutData: Any?) {
    check(content is SupportsPivotTransform) { BadPivotClassErrorMessage }
    content.setTransformPivot(model.first, model.second)
  }

  override fun unbind(
      context: Context,
      content: View,
      model: Pair<Float, Float>,
      layoutData: Any?,
      bindData: Unit?
  ) {
    check(content is SupportsPivotTransform) { BadPivotClassErrorMessage }
    content.resetTransformPivot()
  }
}

/**
 * Sets the degree that this component is rotated around the pivot point. Increasing the value
 * results in clockwise rotation. By default, the pivot point is centered on the component. Setting
 * this property will cause the Component to be represented as a View at mount time if it wasn't
 * going to already.
 *
 * @see android.view.View.setRotation
 * @see pivotPercent to set the pivot point to a percentage of the component's size
 */
inline fun Style.rotation(rotation: Float): Style =
    this + FloatStyleItem(FloatField.ROTATION, rotation)

/**
 * Sets the degree that this component is rotated around the horizontal axis through the pivot
 * point. Setting this property will cause the Component to be represented as a View at mount time
 * if it wasn't going to already.
 *
 * @see android.view.View.setRotationX
 * @see pivotPercent to set the pivot point to a percentage of the component's size
 */
inline fun Style.rotationX(rotationX: Float): Style =
    this + FloatStyleItem(FloatField.ROTATION_X, rotationX)

/**
 * Sets the degree that this component is rotated around the vertical axis through the pivot point.
 * Setting this property will cause the Component to be represented as a View at mount time if it
 * wasn't going to already.
 *
 * @see android.view.View.setRotationY
 * @see pivotPercent to set the pivot point to a percentage of the component's size
 */
inline fun Style.rotationY(rotationY: Float): Style =
    this + FloatStyleItem(FloatField.ROTATION_Y, rotationY)

/**
 * Sets the scale (scaleX and scaleY) on this component. This is mostly relevant for animations and
 * being able to animate size changes. Otherwise for non-animation usecases, you should use the
 * standard layout properties to control the size of your component. Setting this property will
 * cause the Component to be represented as a View at mount time if it wasn't going to already.
 *
 * @see android.view.View.setScaleX
 * @see android.view.View.setScaleY
 * @see pivotPercent to set the pivot point to a percentage of the component's size
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
 * Adds a View tag to the [View] this [Component] mounts to. Setting this property will cause the
 * [Component] to be represented as a [View] at mount time if it wasn't going to already.
 *
 * See [android.view.View.setId]
 */
inline fun Style.viewId(@IdRes viewId: Int): Style =
    this + ObjectStyleItem(ObjectField.VIEW_ID, viewId)

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

/** Defines touch Expansion area around the component on a per-edge basis. */
inline fun Style.touchExpansion(
    all: Dimen? = null,
    horizontal: Dimen? = null,
    vertical: Dimen? = null,
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null,
    left: Dimen? = null,
    right: Dimen? = null,
): Style =
    this +
        all?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_ALL, it) } +
        horizontal?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_HORIZONTAL, it) } +
        vertical?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_VERTICAL, it) } +
        start?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_START, it) } +
        top?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_TOP, it) } +
        end?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_END, it) } +
        bottom?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_BOTTOM, it) } +
        left?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_LEFT, it) } +
        right?.let { DimenStyleItem(DimenField.TOUCH_EXPANSION_RIGHT, it) }

enum class ShadowStyleField : StyleItemField {
  SHADOW_ITEM,
}

@DataClassGenerate
data class ShadowStyleItemParams(
    val elevation: Dimen,
    val outlineProvider: ViewOutlineProvider,
    @ColorInt val ambientShadowColor: Int,
    @ColorInt val spotShadowColor: Int
)

@PublishedApi
@DataClassGenerate
internal data class ShadowStyleItem(
    val elevation: Dimen,
    val outlineProvider: ViewOutlineProvider,
    @ColorInt val ambientShadowColor: Int,
    @ColorInt val spotShadowColor: Int
) : StyleItem<ShadowStyleItemParams> {

  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    commonProps.shadowElevationPx(elevation.toPixels(context.resourceResolver).toFloat())
    commonProps.outlineProvider(outlineProvider)
    commonProps.ambientShadowColor(ambientShadowColor)
    commonProps.spotShadowColor(spotShadowColor)
  }

  override val field: ShadowStyleField = ShadowStyleField.SHADOW_ITEM
  override val value: ShadowStyleItemParams =
      ShadowStyleItemParams(elevation, outlineProvider, ambientShadowColor, spotShadowColor)
}

/**
 * Style for attaching a standard Material Design shadow to a component. Refer to
 * https://material.io/design/environment/light-shadows.html for more information.
 * - **elevation**: Sets the elevation of this component above the surface using
 *   https://developer.android.com/reference/android/view/View#setElevation(float). Larger elevation
 *   values result in larger shadows.
 * - **outlineProvider**: Used to determine the shape of the shadow. If not specified,
 *   https://developer.android.com/reference/android/view/ViewOutlineProvider#BOUNDS will be used to
 *   target the component's bounds.
 * - **ambientShadowColor**: Sets the color of the ambient shadow. Ignored on < API 28 devices. See
 *   https://developer.android.com/reference/android/view/View#setOutlineAmbientShadowColor(int)
 * - **outlineShadowColor**: Sets the color of the spotlight shadow. Ignored on < API 28 devices.
 *   See https://developer.android.com/reference/android/view/View#setOutlineSpotShadowColor(int)
 */
inline fun Style.shadow(
    elevation: Dimen,
    outlineProvider: ViewOutlineProvider = ViewOutlineProvider.BOUNDS,
    @ColorInt ambientShadowColor: Int = Color.BLACK,
    @ColorInt spotShadowColor: Int = Color.BLACK
): Style = this + ShadowStyleItem(elevation, outlineProvider, ambientShadowColor, spotShadowColor)

/**
 * Setting this property will cause the Component to be a keyboard navigation cluster. Setting this
 * property will cause the Component to be represented as a View at mount time if it wasn't going to
 * already.
 *
 * See [android.view.View.setKeyboardNavigationCluster]
 */
inline fun Style.keyboardNavigationCluster(isKeyboardNavigationCluster: Boolean): Style =
    this + ObjectStyleItem(ObjectField.KEYBOARD_NAVIGATION_CLUSTER, isKeyboardNavigationCluster)

/**
 * Excludes a rectangle within the local bounds from the system gesture. After layout, []exclusion]
 * is called to determine the Rect to exclude from the system gesture area.
 *
 * The bounds of the content's location in the layout is passed as passed as exclusion's parameter.
 */
inline fun Style.addSystemGestureExclusionZone(noinline exclusion: (bounds: Rect) -> Rect): Style {
  return this + ObjectStyleItem(ObjectField.ADD_TOUCH_EXCLUSION_ZONE, exclusion)
}

/**
 * Sets the tooltip text which will be displayed in a small popup next to the view.
 *
 * See [android.view.View.setTooltipText]
 */
inline fun Style.tooltipText(tooltipText: String?): Style =
    this + ObjectStyleItem(ObjectField.TOOLTIP_TEXT, tooltipText)
