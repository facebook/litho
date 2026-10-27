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

import com.facebook.litho.layout.LayoutDirection

/**
 * A node's geometry, as the engine that laid it out built it: its size and edges, and where it
 * placed its children. What the pass recorded about measuring its content is on its
 * [LithoLayoutResult]. A leaf's output also keeps engine state a Yoga node keeps: its last
 * measurements and the context of the pass that laid it out.
 *
 * Read-only outside Litho: only its layout engines subclass it. Its internal members are where it
 * placed its children, which its result reports, and what the end of its pass asks of it. Its own
 * position isn't here: its parent's output holds it, and a root's is its tree's offset.
 */
sealed class LithoMeasureOutput {
  abstract val width: Int
  abstract val height: Int
  abstract val paddingLeft: Int
  abstract val paddingTop: Int
  abstract val paddingRight: Int
  abstract val paddingBottom: Int
  abstract val borderLeft: Int
  abstract val borderTop: Int
  abstract val borderRight: Int
  abstract val borderBottom: Int
  abstract val layoutDirection: LayoutDirection

  /**
   * Where this node placed child [index], in its border box: a child's position is its parent's.
   */
  internal abstract fun childX(index: Int): Int

  internal abstract fun childY(index: Int): Int

  /**
   * Drops the layout context of the pass that laid it out, and on a Yoga node its link to the
   * result: only that pass reads them. Its geometry stays.
   */
  internal open fun releaseLayoutContext(): Unit = Unit
}
