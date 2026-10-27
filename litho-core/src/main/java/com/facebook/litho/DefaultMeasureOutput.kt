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

import com.facebook.litho.layout.IntEdges
import com.facebook.litho.layout.LayoutDirection
import com.facebook.yoga.YogaNode

/**
 * A `Layout`'s measure output, written once when its builder builds it: its border box and where it
 * placed its children.
 */
internal class DefaultMeasureOutput(
    override val width: Int,
    override val height: Int,
    private val padding: IntEdges,
    private val border: IntEdges,
    override val layoutDirection: LayoutDirection,
    /** Each built child's left and top within this border box, in its result's child order. */
    private val positions: IntArray,
    /**
     * In a Yoga tree, the node Yoga laid it out with, which holds its style; null laid out on its
     * own or under a `Layout`.
     */
    val yogaNode: YogaNode? = null,
) : LithoMeasureOutput() {

  override val paddingLeft: Int
    get() = padding.left

  override val paddingTop: Int
    get() = padding.top

  override val paddingRight: Int
    get() = padding.right

  override val paddingBottom: Int
    get() = padding.bottom

  override val borderLeft: Int
    get() = border.left

  override val borderTop: Int
    get() = border.top

  override val borderRight: Int
    get() = border.right

  override val borderBottom: Int
    get() = border.bottom

  override fun childX(index: Int): Int = positions[index * 2]

  override fun childY(index: Int): Int = positions[index * 2 + 1]

  override fun releaseLayoutContext() {
    yogaNode?.data = null
  }
}

/** The positions of a `Layout` that built no child. */
internal val NO_POSITIONS: IntArray = IntArray(0)
