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
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutContext

/**
 * The measure output of a `Layout`'s leaf child, which its builder measures with no Yoga node: its
 * content's last eight measurements, the newest replacing the oldest, its border box, set when it's
 * built, and the context of the pass that laid it out, which a Yoga node keeps in its data.
 */
internal class LeafLayoutOutput(
    override val layoutDirection: LayoutDirection,
    layoutContext: LayoutContext<LithoLayoutContext>,
) : LithoMeasureOutput() {

  override var width: Int = 0
    private set

  override var height: Int = 0
    private set

  private var padding: IntEdges = IntEdges.None

  private var border: IntEdges = IntEdges.None

  /**
   * What `onBoundsDefined` measures a Primitive with when it wasn't measured at its final size;
   * null once the pass is released.
   */
  var layoutContext: LayoutContext<LithoLayoutContext>? = layoutContext
    private set

  /** The width spec its content was last measured under. */
  private var lastWidthSpec: Int = 0

  /** The height spec its content was last measured under. */
  private var lastHeightSpec: Int = 0

  /** The content size it measured under those specs, or [IntSize.Invalid] before it's measured. */
  private var lastContentSize: IntSize = IntSize.Invalid

  /**
   * Its measurements before the latest, oldest first, four ints each: width spec, height spec,
   * content width and content height. Null until it's measured under a second pair of specs. A new
   * measurement makes a new array, so a copy from the layout cache shares it.
   */
  private var earlierMeasurements: IntArray? = null

  /**
   * The content size of the first of its last [MAX_CACHED_MEASUREMENTS] measurements that stands
   * for [widthSpec] and [heightSpec], or [IntSize.Invalid], trying its latest measurement first,
   * then the earlier ones oldest first. One stands if, on each axis, its spec is equal, as Yoga's
   * cache reuses, or, when [matchesCompatibleSpecs], compatible
   * (`LithoYogaLayoutFunction.isSpecCompatible`), as a DiffNode does. Its layout data stays its
   * latest measurement's, as on a Yoga cache hit: `onBoundsDefined` corrects it for the size it's
   * built at.
   */
  fun contentMeasuredUnder(
      widthSpec: Int,
      heightSpec: Int,
      matchesCompatibleSpecs: Boolean,
  ): IntSize {
    if (
        lastContentSize.isValid &&
            stands(lastWidthSpec, widthSpec, lastContentSize.width, matchesCompatibleSpecs) &&
            stands(lastHeightSpec, heightSpec, lastContentSize.height, matchesCompatibleSpecs)
    ) {
      return lastContentSize
    }
    val earlierMeasurements = earlierMeasurements ?: return IntSize.Invalid
    var i = 0
    while (i < earlierMeasurements.size) {
      if (
          stands(
              earlierMeasurements[i],
              widthSpec,
              earlierMeasurements[i + 2],
              matchesCompatibleSpecs,
          ) &&
              stands(
                  earlierMeasurements[i + 1],
                  heightSpec,
                  earlierMeasurements[i + 3],
                  matchesCompatibleSpecs,
              )
      ) {
        return IntSize(earlierMeasurements[i + 2], earlierMeasurements[i + 3])
      }
      i += 4
    }
    return IntSize.Invalid
  }

  /** Whether a measurement of [measuredSize] under [oldSpec] stands for [newSpec] on its axis. */
  private fun stands(
      oldSpec: Int,
      newSpec: Int,
      measuredSize: Int,
      matchesCompatibleSpecs: Boolean,
  ): Boolean =
      oldSpec == newSpec ||
          (matchesCompatibleSpecs &&
              LithoYogaLayoutFunction.isSpecCompatible(oldSpec, newSpec, measuredSize))

  /**
   * Records [content] as measured under [widthSpec] and [heightSpec], which none of its kept
   * measurements stands for, keeping its latest among the earlier ones, without the oldest when it
   * holds [MAX_CACHED_MEASUREMENTS].
   */
  fun recordMeasurement(widthSpec: Int, heightSpec: Int, content: IntSize) {
    if (lastContentSize.isValid) {
      val earlierMeasurements = earlierMeasurements ?: IntArray(0)
      val dropped = if (earlierMeasurements.size == (MAX_CACHED_MEASUREMENTS - 1) * 4) 4 else 0
      val measurements = IntArray(earlierMeasurements.size - dropped + 4)
      earlierMeasurements.copyInto(measurements, destinationOffset = 0, startIndex = dropped)
      measurements[measurements.size - 4] = lastWidthSpec
      measurements[measurements.size - 3] = lastHeightSpec
      measurements[measurements.size - 2] = lastContentSize.width
      measurements[measurements.size - 1] = lastContentSize.height
      this.earlierMeasurements = measurements
    }
    lastWidthSpec = widthSpec
    lastHeightSpec = heightSpec
    lastContentSize = content
  }

  /** Its border box: [size], inside [padding] and [border]. */
  fun setBorderBox(size: IntSize, padding: IntEdges, border: IntEdges) {
    width = size.width
    height = size.height
    this.padding = padding
    this.border = border
  }

  /**
   * A copy for a pass that measures under [layoutContext], with this output's last box and
   * measurements, as Yoga's clone of a cached node keeps its layout: content measured again before
   * it's built sees the padding it had.
   */
  fun cachedCopy(layoutContext: LayoutContext<LithoLayoutContext>): LeafLayoutOutput =
      LeafLayoutOutput(layoutDirection, layoutContext).also { copy ->
        copy.setBorderBox(IntSize(width, height), padding, border)
        copy.lastWidthSpec = lastWidthSpec
        copy.lastHeightSpec = lastHeightSpec
        copy.lastContentSize = lastContentSize
        copy.earlierMeasurements = earlierMeasurements
      }

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

  override fun childX(index: Int): Int = error("A leaf has no child $index")

  override fun childY(index: Int): Int = error("A leaf has no child $index")

  override fun releaseLayoutContext() {
    layoutContext = null
  }

  private companion object {
    /** As Yoga's `LayoutResults::MaxCachedMeasurements`. */
    const val MAX_CACHED_MEASUREMENTS: Int = 8
  }
}
