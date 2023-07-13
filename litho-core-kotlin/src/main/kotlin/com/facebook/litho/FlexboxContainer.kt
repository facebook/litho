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

import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaWrap

/**
 * Constructs a new [Column]. Add children by using [FlexboxContainerScope.child] or
 * [FlexboxContainerScope.children]:
 * ```
 * Column(...) {
 *   child(Text(text = "Foo"))
 * }
 * ```
 */
@Suppress("FunctionName")
inline fun ResourcesScope.Column(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    style: Style? = null,
    init: FlexboxContainerScope.() -> Unit,
): Column {
  val containerScope = FlexboxContainerScope(context)
  containerScope.init()
  return createColumn(
      alignContent, alignItems, justifyContent, wrap, isReversed, style, containerScope)
}

/** Constructs a new [Column] without any children. */
@Suppress("FunctionName")
inline fun ResourcesScope.Column(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    style: Style? = null
): Column = createColumn(alignContent, alignItems, justifyContent, wrap, isReversed, style, null)

/** Internal function to allow [Column] to be inlineable. */
fun ResourcesScope.createColumn(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    style: Style? = null,
    resolvedContainerScope: FlexboxContainerScope?
): Column =
    Column(
            alignContent,
            alignItems,
            justifyContent,
            wrap,
            isReversed,
            resolvedContainerScope?.children)
        .apply {
          style?.applyCommonProps(
              context, (this as SpecGeneratedComponent).getOrCreateCommonProps())
        }

/**
 * Constructs a new [Row]. Add children by using [FlexboxContainerScope.child] or
 * [FlexboxContainerScope.children]:
 * ```
 * Row(...) {
 *   child(Text(text = "Foo"))
 * }
 * ```
 */
@Suppress("FunctionName")
inline fun ResourcesScope.Row(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    style: Style? = null,
    init: FlexboxContainerScope.() -> Unit,
): Row {
  val containerScope = FlexboxContainerScope(context)
  containerScope.init()
  return createRow(
      alignContent, alignItems, justifyContent, wrap, isReversed, style, containerScope)
}

/** Constructs a new [Column] without any children. */
@Suppress("FunctionName")
inline fun ResourcesScope.Row(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    style: Style? = null
): Row = createRow(alignContent, alignItems, justifyContent, wrap, isReversed, style, null)

/** Internal function to allow [Row] to be inlineable. */
fun ResourcesScope.createRow(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    style: Style? = null,
    resolvedContainerScope: FlexboxContainerScope?
): Row =
    Row(
            alignContent,
            alignItems,
            justifyContent,
            wrap,
            isReversed,
            resolvedContainerScope?.children)
        .apply {
          style?.applyCommonProps(
              context, (this as SpecGeneratedComponent).getOrCreateCommonProps())
        }

/**
 * The implicit receiver for the trailing lambda on [ResourcesScope.Column] or [ResourcesScope.Row].
 * The receiver gives the ability to add children to this container.
 */
@ContainerDsl
class FlexboxContainerScope(
    override val context: ComponentContext,
    internal val children: MutableList<Component> = mutableListOf(),
) : ResourcesScope {

  /** Adds a Component as a child to the Row or Column being initialized. */
  fun child(component: Component?) {
    component?.let { children.add(component) }
  }
}
