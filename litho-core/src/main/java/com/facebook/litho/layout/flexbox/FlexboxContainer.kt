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

package com.facebook.litho.layout.flexbox

import com.facebook.litho.Component
import com.facebook.litho.Style
import com.facebook.rendercore.Dimen
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaWrap

/**
 * Receiver for the trailing lambda on [Row] and [Column]. Exposes [child] to add children to the
 * container being built.
 *
 * This scope does not implement `ResourcesScope`. The container construction does not need a
 * [com.facebook.litho.ComponentContext] — gap [Dimen] values are resolved to pixels at layout time.
 * Resource lookups (`colorRes`, `drawableRes`, `stringRes`) called inside the children block
 * resolve against the enclosing `ResourcesScope` (e.g., `ComponentScope` inside
 * `KComponent.render`).
 *
 * Annotated with [FlexboxContainerDsl] — a marker distinct from `com.facebook.litho.ContainerDsl`
 * so that, inside nested `Row { Row { … } }` chains, [child] resolves against the innermost
 * [FlexboxScope] only, while outer `ResourcesScope` extensions (carried by `ComponentScope`,
 * `LazyListScope`, etc., which use a different — or no — marker) remain reachable from the children
 * block.
 */
@FlexboxContainerDsl
class FlexboxScope @PublishedApi internal constructor() {

  @PublishedApi internal val children: MutableList<Component> = mutableListOf()

  /** Adds a [Component] as a child of the [Row] or [Column] being built. */
  fun child(component: Component?) {
    component?.let { children.add(it) }
  }
}

/**
 * Constructs a [KRow] without requiring a [com.facebook.litho.ComponentContext] or
 * [com.facebook.litho.ResourcesScope].
 *
 * ```
 * Row(style = Style.padding(8.dp), alignItems = YogaAlign.CENTER) {
 *   child(Text(text = "Hello"))
 * }
 * ```
 *
 * @param alignContent Defines how the lines of children are distributed along the cross-axis when
 *   there is extra space (only applies when [wrap] is enabled). Uses [YogaAlign] values such as
 *   `FLEX_START`, `CENTER`, `FLEX_END`, `STRETCH`, `SPACE_BETWEEN`, or `SPACE_AROUND`.
 * @param alignItems Defines the default alignment for children along the cross-axis.
 * @param justifyContent Defines how children are distributed along the main axis (horizontal for
 *   Row).
 * @param wrap Controls whether children wrap to multiple lines when they exceed the container's
 *   size.
 * @param isReversed If true, reverses the order of children along the main axis (right-to-left
 *   instead of left-to-right).
 * @param gap Specifies the spacing between children. Applied uniformly between all adjacent
 *   children in the container. Resolved to pixels at layout time.
 * @param mainAxisGap Spacing between children along the main axis (horizontal for Row). Takes
 *   precedence over [gap] if both are specified.
 * @param crossAxisGap Spacing between wrapped lines along the cross axis (vertical for Row). Only
 *   applies when [wrap] is enabled. Takes precedence over [gap] if both are specified.
 * @param style The [Style] to apply to this Row for additional layout and visual properties.
 * @param customSimpleName Overrides the component's [Component.getSimpleName] (defaults to "Row").
 *   Preserves the debug/finder identity of legacy `Row.create(c, name)` call sites migrated onto
 *   this DSL; leave null for the standard "Row" name.
 * @param init A lambda with [FlexboxScope] receiver for adding children to this Row using
 *   [FlexboxScope.child].
 */
@Suppress("FunctionName")
inline fun Row(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    gap: Dimen? = null,
    mainAxisGap: Dimen? = null,
    crossAxisGap: Dimen? = null,
    style: Style = Style,
    customSimpleName: String? = null,
    init: FlexboxScope.() -> Unit = {},
): KRow {
  val scope = FlexboxScope()
  scope.init()
  return KRow(
      alignContent = alignContent,
      alignItems = alignItems,
      justifyContent = justifyContent,
      wrap = wrap,
      isReversed = isReversed,
      gap = gap,
      mainAxisGap = mainAxisGap,
      crossAxisGap = crossAxisGap,
      children = scope.children,
      style = style,
      customSimpleName = customSimpleName,
  )
}

/**
 * Constructs a [KColumn] without requiring a [com.facebook.litho.ComponentContext] or
 * [com.facebook.litho.ResourcesScope].
 *
 * ```
 * Column(style = Style.padding(8.dp)) {
 *   child(Text(text = "Hello"))
 * }
 * ```
 *
 * @param alignContent Defines how the lines of children are distributed along the cross-axis when
 *   there is extra space (only applies when [wrap] is enabled).
 * @param alignItems Defines the default alignment for children along the cross-axis.
 * @param justifyContent Defines how children are distributed along the main axis (vertical for
 *   Column).
 * @param wrap Controls whether children wrap to multiple lines when they exceed the container's
 *   size.
 * @param isReversed If true, reverses the order of children along the main axis (bottom-to-top
 *   instead of top-to-bottom).
 * @param gap Specifies the spacing between children. Applied uniformly between all adjacent
 *   children in the container. Resolved to pixels at layout time.
 * @param mainAxisGap Spacing between children along the main axis (vertical for Column). Takes
 *   precedence over [gap] if both are specified.
 * @param crossAxisGap Spacing between wrapped lines along the cross axis (horizontal for Column).
 *   Only applies when [wrap] is enabled. Takes precedence over [gap] if both are specified.
 * @param style The [Style] to apply to this Column for additional layout and visual properties.
 * @param customSimpleName Overrides the component's [Component.getSimpleName] (defaults to
 *   "Column"). Preserves the debug/finder identity of legacy `Column.create(c, name)` call sites
 *   migrated onto this DSL; leave null for the standard "Column" name.
 * @param init A lambda with [FlexboxScope] receiver for adding children to this Column using
 *   [FlexboxScope.child].
 */
@Suppress("FunctionName")
inline fun Column(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    gap: Dimen? = null,
    mainAxisGap: Dimen? = null,
    crossAxisGap: Dimen? = null,
    style: Style = Style,
    customSimpleName: String? = null,
    init: FlexboxScope.() -> Unit = {},
): KColumn {
  val scope = FlexboxScope()
  scope.init()
  return KColumn(
      alignContent = alignContent,
      alignItems = alignItems,
      justifyContent = justifyContent,
      wrap = wrap,
      isReversed = isReversed,
      gap = gap,
      mainAxisGap = mainAxisGap,
      crossAxisGap = crossAxisGap,
      children = scope.children,
      style = style,
      customSimpleName = customSimpleName,
  )
}
