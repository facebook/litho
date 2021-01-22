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

import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaWrap

/**
 * [ColumnWithoutChildren] and [RowWithoutChildren] support the "bracket children" API. As one might
 * expect, they render to an immutable Row/Column without any children. However their main purpose
 * is to expose the `operator fun get()` override that allows adding children in the Litho kotlin
 * DSL without adding it to all Rows/Columns (i.e. ones that may already have children) and without
 * needing to import an extension function.
 *
 * The operator returns a new Row/Column with these children so that everything stays immutable.
 */
class ColumnWithoutChildren
internal constructor(
    private val resourceResolver: ResourceResolver,
    private val alignContent: YogaAlign? = null,
    private val alignItems: YogaAlign? = null,
    private val justifyContent: YogaJustify? = null,
    private val wrap: YogaWrap? = null,
    private val isReversed: Boolean = false,
    private val style: Style? = null,
) :
    KComponent({
      com.facebook.litho.Column(alignContent, alignItems, justifyContent, wrap, isReversed).apply {
        applyStyle(style)
      }
    }) {

  /** Returns a new Column with the same props and the given children. */
  operator fun get(vararg children: Component?): Column =
      Column(
              alignContent,
              alignItems,
              justifyContent,
              wrap,
              isReversed,
              toNonNullComponentList(children))
          .apply { style?.applyToProps(resourceResolver, getOrCreateCommonProps()) }

  /**
   * Returns a new Column with the same props and the given children, but as a pre-constructed list.
   */
  operator fun get(children: List<out Component?>): Column =
      Column(
              alignContent,
              alignItems,
              justifyContent,
              wrap,
              isReversed,
              toNonNullComponentList(children))
          .apply { style?.applyToProps(resourceResolver, getOrCreateCommonProps()) }
}

/**
 * Constructs a new [com.facebook.litho.Column]. Add children via trailing `[]`, e.g.
 * ```
 * Column(...) [
 *   Text(text = "Foo"),
 * ]
 * ```
 */
@Suppress("FunctionName")
fun DslScope.Column(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    style: Style? = null,
): ColumnWithoutChildren =
    ColumnWithoutChildren(
        resourceResolver, alignContent, alignItems, justifyContent, wrap, isReversed, style)

/** See docs on [ColumnWithoutChildren]. */
class RowWithoutChildren
internal constructor(
    private val resourceResolver: ResourceResolver,
    private val alignContent: YogaAlign? = null,
    private val alignItems: YogaAlign? = null,
    private val justifyContent: YogaJustify? = null,
    private val wrap: YogaWrap? = null,
    private val isReversed: Boolean = false,
    private val style: Style? = null,
) :
    KComponent({
      com.facebook.litho.Row(alignContent, alignItems, justifyContent, wrap, isReversed).apply {
        applyStyle(style)
      }
    }) {

  /** Returns a new Column with the same props and the given children. */
  operator fun get(vararg children: Component?): Row =
      Row(
              alignContent,
              alignItems,
              justifyContent,
              wrap,
              isReversed,
              toNonNullComponentList(children))
          .apply { style?.applyToProps(resourceResolver, getOrCreateCommonProps()) }

  /**
   * Returns a new Column with the same props and the given children, but as a pre-constructed list.
   */
  operator fun get(children: List<Component?>): Row =
      Row(
              alignContent,
              alignItems,
              justifyContent,
              wrap,
              isReversed,
              toNonNullComponentList(children))
          .apply { style?.applyToProps(resourceResolver, getOrCreateCommonProps()) }
}

/**
 * Constructs a new [com.facebook.litho.Row]. Add children via trailing `[]`, e.g.
 * ```
 * Row(...) [
 *   Text(text = "Foo"),
 * ]
 * ```
 */
@Suppress("FunctionName")
fun DslScope.Row(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    isReversed: Boolean = false,
    style: Style? = null,
): RowWithoutChildren =
    RowWithoutChildren(
        resourceResolver, alignContent, alignItems, justifyContent, wrap, isReversed, style)

/**
 * These functions are the functional equivalent of `filterNotNull`. They exist since we know
 * exactly the number of children we have and in lower-end phones have seen OOM impact in reducing
 * over-allocation of long-lived ArrayLists.
 */
private inline fun toNonNullComponentList(children: Array<out Component?>): List<Component> {
  val size = children.count { it != null }
  val out = ArrayList<Component>(size)
  children.forEach { child -> child?.let { out.add(it) } }
  return out
}

private inline fun toNonNullComponentList(children: List<out Component?>): List<Component> {
  val size = children.count { it != null }
  val out = ArrayList<Component>(size)
  children.forEach { child -> child?.let { out.add(it) } }
  return out
}
