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

package com.facebook.litho.kotlin.widget

import android.graphics.Path
import android.graphics.PathDashPathEffect
import androidx.annotation.ColorInt
import com.facebook.litho.Border
import com.facebook.litho.ResourcesScope
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.ResourceResolver
import com.facebook.yoga.YogaEdge

/**
 * Creates a border style to be applied to a component
 *
 * @param edgeAll sets the props for all edges.
 * @param edgeHorizontal customises both horizontal edges.
 * @param edgeVertical customises both vertical edges.
 * @param edgeStart customises the start edge, from top to bottom.
 * @param edgeEnd customises end edge, from top to bottom.
 * @param edgeTop customises the top edge of the border.
 * @param edgeBottom customises the bottom edge of the border.
 * @param edgeLeft customises the left edge of the border.
 * @param edgeRight customises for the right edge of the border.
 * @param radius customises each corner radius.
 * @param effect applies an effect to the border.
 */
inline fun ResourcesScope.Border(
    // separate edge properties
    edgeAll: BorderEdge? = null,

    // misc
    edgeHorizontal: BorderEdge? = null,
    edgeVertical: BorderEdge? = null,
    edgeStart: BorderEdge? = null,
    edgeEnd: BorderEdge? = null,

    // standard edges
    edgeTop: BorderEdge? = null,
    edgeBottom: BorderEdge? = null,
    edgeLeft: BorderEdge? = null,
    edgeRight: BorderEdge? = null,

    // radius properties
    radius: BorderRadius? = null,

    // extra path effects
    effect: BorderEffect? = null
): Border =
    Border.create(context)
        .apply {
          edgeAll?.let { it.apply(YogaEdge.ALL, this, resourceResolver) }

          edgeHorizontal?.let { it.apply(YogaEdge.HORIZONTAL, this, resourceResolver) }
          edgeVertical?.let { it.apply(YogaEdge.VERTICAL, this, resourceResolver) }
          edgeStart?.let { it.apply(YogaEdge.START, this, resourceResolver) }
          edgeEnd?.let { it.apply(YogaEdge.END, this, resourceResolver) }

          edgeTop?.let { it.apply(YogaEdge.TOP, this, resourceResolver) }
          edgeBottom?.let { it.apply(YogaEdge.BOTTOM, this, resourceResolver) }
          edgeLeft?.let { it.apply(YogaEdge.LEFT, this, resourceResolver) }
          edgeRight?.let { it.apply(YogaEdge.RIGHT, this, resourceResolver) }

          radius?.let { it.apply(this, resourceResolver) }

          effect?.let { it.apply(this) }
        }
        .build()

/**
 * Stores properties that are used to customise border edges
 *
 * @param color the colour to be applied to the edge
 * @param width the width of the edge
 */
class BorderEdge(@ColorInt val color: Int? = null, val width: Dimen? = null) {

  @PublishedApi
  internal inline fun apply(
      edge: YogaEdge,
      builder: Border.Builder,
      resourceResolver: ResourceResolver
  ) {
    color?.let { builder.color(edge, it) }
    width?.let { builder.widthPx(edge, it.toPixels(resourceResolver)) }
  }
}

/**
 * Stores properties that are used to customise the border radii
 *
 * @param all the radius to be applied to all corners
 * @param topLeft the radius to be applied to the top left corner
 * @param topRight the radius to be applied to the top right corner
 * @param bottomLeft the radius to be applied to the bottom left corner
 * @param bottomRight the radius to be applied to the bottom right corner
 */
class BorderRadius(
    val all: Dimen? = null,
    val topLeft: Dimen? = null,
    val topRight: Dimen? = null,
    val bottomLeft: Dimen? = null,
    val bottomRight: Dimen? = null,
) {
  @PublishedApi
  internal inline fun apply(builder: Border.Builder, resourceResolver: ResourceResolver) {
    all?.let { builder.radiusPx(it.toPixels(resourceResolver)) }
    topLeft?.let { builder.radiusPx(Border.Corner.TOP_LEFT, it.toPixels(resourceResolver)) }
    topRight?.let { builder.radiusPx(Border.Corner.TOP_RIGHT, it.toPixels(resourceResolver)) }
    bottomLeft?.let { builder.radiusPx(Border.Corner.BOTTOM_LEFT, it.toPixels(resourceResolver)) }
    bottomRight?.let { builder.radiusPx(Border.Corner.BOTTOM_RIGHT, it.toPixels(resourceResolver)) }
  }
}

/**
 * Stores properties that are used to customise an effect that is applied to the border.
 *
 * @see BorderEffect.path
 * @see BorderEffect.dashed
 * @see BorderEffect.discrete
 */
class BorderEffect {

  // shared properties
  var phase: Float? = null

  // dash properties
  var intervals: FloatArray? = null

  // path properties
  var pathShape: Path? = null
  var pathAdvance: Float? = null
  var pathStyle: PathDashPathEffect.Style? = null

  // discrete effect properties
  var segmentLength: Float? = null
  var deviation: Float? = null

  private constructor(intervals: FloatArray, phase: Float) {
    this.intervals = intervals
    this.phase = phase
  }

  private constructor(
      pathShape: Path,
      pathAdvance: Float,
      pathPhase: Float,
      pathStyle: PathDashPathEffect.Style
  ) {
    this.pathShape = pathShape
    this.pathAdvance = pathAdvance
    this.phase = pathPhase
    this.pathStyle = pathStyle
  }

  private constructor(segmentLength: Float, deviation: Float) {
    this.segmentLength = segmentLength
    this.deviation = deviation
  }

  @PublishedApi
  internal inline fun apply(builder: Border.Builder) {
    // apply dashed border effect.
    if (this.intervals != null) {
      builder.dashEffect(intervals, phase ?: 0f)
    }

    // apply path border effect.
    if (this.pathShape != null) {
      builder.pathDashEffect(pathShape, pathAdvance ?: 0f, phase ?: 0f, pathStyle)
    }

    // apply discrete path border effect.
    if (this.segmentLength != null) {
      builder.discreteEffect(segmentLength ?: 0f, deviation ?: 0f)
    }
  }

  companion object {

    /**
     * Applies a path dash effect to the border
     *
     * @see Border.Builder.pathDashEffect
     */
    fun path(
        shape: Path,
        advance: Float,
        phase: Float,
        style: PathDashPathEffect.Style
    ): BorderEffect {
      return BorderEffect(shape, advance, phase, style)
    }

    /**
     * Applies a dash effect to the border.
     *
     * @see Border.Builder.dashEffect
     */
    fun dashed(intervals: FloatArray, phase: Float): BorderEffect {
      return BorderEffect(intervals, phase)
    }

    /**
     * Applies a discrete effect to the border.
     *
     * @see Border.Builder.discreteEffect
     */
    fun discrete(segmentLength: Float, deviation: Float): BorderEffect {
      return BorderEffect(segmentLength, deviation)
    }
  }
}
