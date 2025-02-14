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

package com.facebook.litho.widget.collection

/**
 * Specifies how a [Collection] will wrap its contents across the cross axis. For example, in a
 * horizontal list, the cross axis is vertical, meaning this enum controls how the Collection will
 * determine its height.
 */
enum class CrossAxisWrapMode {
  /** No wrapping specified. The size should be specified on the [Collection]'s style parameter. */
  NoWrap,

  /** The cross axis dimension will match the first child in the [Collection] */
  MatchFirstChild,

  /**
   * The cross axis dimension will match the largest item seen so far in the [Collection]. The
   * dimension is adjusted dynamically (ie: during scrolling). Measuring all the children comes with
   * a high performance cost, especially for infinite scrolls. This should only be used if
   * absolutely necessary.
   *
   * This is an experimental feature and your Section surface will take a perf hit if you use it.
   *
   * <p>Whether the items of this RecyclerBinder can change height after the initial measure. Only
   * applicable to horizontally scrolling RecyclerBinders. If true, the children of this h-scroll
   * are all measured with unspecified height. When the ComponentTree of a child is remeasured, this
   * will cause the RecyclerBinder to remeasure in case the height of the child changed and the
   * RecyclerView needs to have a different height to account for it. This only supports changing
   * the height of the item that triggered the remeasuring, not the height of all items in the
   * h-scroll.
   */
  Dynamic,
}
