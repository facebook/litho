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

@file:Suppress("NOTHING_TO_INLINE", "FunctionName")

package com.facebook.litho

import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaWrap

inline fun DslScope.Column(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    reverse: Boolean = false,
    content: DslContainerScope.() -> Unit = {}
): Column =
    Column.create(context)
        .apply {
          alignContent?.let { alignContent(it) }
          alignItems?.let { alignItems(it) }
          justifyContent?.let { justifyContent(it) }
          wrap?.let { wrap(it) }
          if (reverse) {
            reverse(reverse)
          }
          DslContainerScope(this).content()
        }
        .build()

inline fun DslScope.Row(
    alignContent: YogaAlign? = null,
    alignItems: YogaAlign? = null,
    justifyContent: YogaJustify? = null,
    wrap: YogaWrap? = null,
    reverse: Boolean = false,
    content: DslContainerScope.() -> Unit = {}
): Row =
    Row.create(context)
        .apply {
          alignContent?.let { alignContent(it) }
          alignItems?.let { alignItems(it) }
          justifyContent?.let { justifyContent(it) }
          wrap?.let { wrap(it) }
          if (reverse) {
            reverse(reverse)
          }
          DslContainerScope(this).content()
        }
        .build()

/**
 * A scope that exposes only [unaryPlus] operator in the context of [Component.ContainerBuilder]
 * containers for adding children.
 */
inline class DslContainerScope(private val container: Component.ContainerBuilder<*>) {
  operator fun Component.Builder<*>?.unaryPlus() {
    container.child(this)
  }

  operator fun Component?.unaryPlus() {
    container.child(this)
  }
}
