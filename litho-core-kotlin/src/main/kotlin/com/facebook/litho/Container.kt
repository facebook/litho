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

@file:Suppress("NOTHING_TO_INLINE")

package com.facebook.litho

import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaWrap

inline fun ComponentContext.Column(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    reverse: Boolean = false,
    content: DslColumnBuilder.() -> Unit
): Column.Builder =
    DslColumnBuilder(this).apply {
      alignContent(alignContent)
      alignItems(alignItems)
      justifyContent(justifyContent)
      wrap(wrap)
      reverse(reverse)
      content()
    }

inline fun ComponentContext.Row(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    reverse: Boolean = false,
    content: DslRowBuilder.() -> Unit
): Row.Builder =
    DslRowBuilder(this).apply {
      alignContent(alignContent)
      alignItems(alignItems)
      justifyContent(justifyContent)
      wrap(wrap)
      reverse(reverse)
      content()
    }

/** Custom [Row.Builder] that exposes [unaryPlus] operator in its context for adding children. */
class DslRowBuilder(c: ComponentContext) : Row.Builder() {
  init {
    init(c, 0, 0, Row("Row"))
  }

  inline operator fun Component.Builder<*>?.unaryPlus() {
    child(this)
  }

  inline operator fun Component?.unaryPlus() {
    child(this)
  }
}

/** Custom [Column.Builder] that exposes [unaryPlus] operator in its context for adding children. */
class DslColumnBuilder(c: ComponentContext) : Column.Builder() {
  init {
    init(c, 0, 0, Column("Column"))
  }

  inline operator fun Component.Builder<*>?.unaryPlus() {
    child(this)
  }

  inline operator fun Component?.unaryPlus() {
    child(this)
  }
}
