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

import android.graphics.Rect
import androidx.collection.ScatterSet
import com.facebook.litho.layout.IntEdges
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.state.StateId
import com.facebook.rendercore.LayoutResult

/**
 * A `Layout`'s output, written once when its builder builds it: its border box, its children's
 * positions, and what it carries out of the pass.
 */
internal class CustomLayoutOutput(
    override val width: Int,
    override val height: Int,
    private val padding: IntEdges,
    private val border: IntEdges,
    override val layoutDirection: LayoutDirection,
    /** Each built child's left and top within this border box, in its result's child order. */
    internal val childPositions: IntArray,
    /** The state its function and placement read, and the reads of the children it discarded. */
    internal val stateReads: ScatterSet<StateId>?,
    /** What its function or placement threw, reported only if it reaches the final tree. */
    internal val pendingFailure: Exception?,
) : LithoLayoutOutput {

  override val x: Int
    get() = TODO()

  override val y: Int
    get() = TODO()

  override val contentWidth: Int
    get() = (width - padding.horizontal - border.horizontal).coerceAtLeast(0)

  override val contentHeight: Int
    get() = (height - padding.vertical - border.vertical).coerceAtLeast(0)

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

  override val isCachedLayout: Boolean
    get() = TODO()

  override val diffNode: DiffNode?
    get() = TODO()

  override val widthSpec: Int
    get() = TODO()

  override val heightSpec: Int
    get() = TODO()

  override val lastMeasuredSize: Long
    get() = TODO()

  override val layoutData: Any?
    get() = TODO()

  override val wasMeasured: Boolean
    get() = TODO()

  override val cachedMeasuresValid: Boolean
    get() = TODO()

  override val measureHadExceptions: Boolean
    get() = TODO()

  override val contentRenderUnit: LithoRenderUnit?
    get() = TODO()

  override val hostRenderUnit: LithoRenderUnit?
    get() = TODO()

  override val borderRenderUnit: LithoRenderUnit?
    get() = TODO()

  override val effects: List<Attachable>?
    get() = TODO()

  override val delegate: LayoutResult?
    get() = TODO()

  override val actualDeferredNodeResult: LithoLayoutResult?
    get() = TODO()

  override val adjustedBounds: Rect
    get() = TODO()
}

/** The positions of a `Layout` that built no child. */
internal val NO_POSITIONS: IntArray = IntArray(0)
