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
 * Constructs a new [com.facebook.litho.Column]. Add children by passing an immutable list as the
 * children prop e.g.
 * ```
 * Column(..., children = listOf(
 *   Text(text = "Foo"),
 * ))
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
    children: List<Component?>? = null
): Column =
    Column(alignContent, alignItems, justifyContent, wrap, isReversed, children).apply {
      style?.applyToComponent(resourceResolver, this)
    }

/**
 * Constructs a new [com.facebook.litho.Row]. Add children by passing an immutable list as the
 * children prop e.g.
 * ```
 * Row(..., children = listOf(
 *   Text(text = "Foo"),
 * ))
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
    children: List<Component?>? = null
): Row =
    Row(alignContent, alignItems, justifyContent, wrap, isReversed, children).apply {
      style?.applyToComponent(resourceResolver, this)
    }
