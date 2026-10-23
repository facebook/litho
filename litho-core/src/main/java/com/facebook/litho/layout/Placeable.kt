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

package com.facebook.litho.layout

import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints

/**
 * A child that has been laid out and knows its size, but not yet where it sits.
 *
 * Only the framework can make one — the constructor is internal — which is what lets
 * [PlacementScope.place] trust its child. `place` is an extension on this type rather than a
 * function taking an index, so the index travels with the placeable and cannot be fabricated to
 * place a child at the wrong slot.
 */
@ExperimentalLithoApi
class Placeable
internal constructor(
    /**
     * The child measured. Framework-only: its index is how `place` finds a slot, and its owner the
     * only container that may place it.
     */
    internal val measurable: Measurable,
    /** The constraints this child was measured under. */
    internal val constraints: SizeConstraints,
    /** What the child measured, margins included, kept the moment its layout returned. */
    measured: IntSize,
    /** Where the child's border box sits inside [measuredWidth]: its left and top margins. */
    internal val marginLeft: Int,
    internal val marginTop: Int,
    /** The layout this measurement made, which is what mounts if it is placed. */
    internal val result: LithoLayoutResult,
) {

  /**
   * The width this child asked for, margins included, before it was kept within its constraints.
   * Differs from [width] only when the child does not fit them: it is then centred in [width].
   */
  val measuredWidth: Int = measured.width

  /** The height this child asked for. The other axis of [measuredWidth]. */
  val measuredHeight: Int = measured.height

  /** The width this child occupies, margins included, within the constraints it was measured at. */
  val width: Int = measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)

  /**
   * The height this child occupies, margins included, within the constraints it was measured at.
   */
  val height: Int = measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

  /** How far this child is shifted to centre [measuredWidth] in [width]. */
  internal val offsetX: Int
    get() = (width - measuredWidth) / 2

  /** How far this child is shifted to centre [measuredHeight] in [height]. */
  internal val offsetY: Int
    get() = (height - measuredHeight) / 2
}
