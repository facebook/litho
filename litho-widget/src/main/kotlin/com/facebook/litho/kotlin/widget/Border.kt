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

import androidx.annotation.ColorInt
import com.facebook.litho.Border
import com.facebook.litho.ResourcesScope
import com.facebook.litho.flexbox.BorderEffect
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
fun ResourcesScope.Border(
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
    effect: BorderEffect? = null,
): Border =
    Border.create(context)
        .apply {
          edgeAll?.apply(YogaEdge.ALL, this, resourceResolver)

          edgeHorizontal?.apply(YogaEdge.HORIZONTAL, this, resourceResolver)
          edgeVertical?.apply(YogaEdge.VERTICAL, this, resourceResolver)
          edgeStart?.apply(YogaEdge.START, this, resourceResolver)
          edgeEnd?.apply(YogaEdge.END, this, resourceResolver)

          edgeTop?.apply(YogaEdge.TOP, this, resourceResolver)
          edgeBottom?.apply(YogaEdge.BOTTOM, this, resourceResolver)
          edgeLeft?.apply(YogaEdge.LEFT, this, resourceResolver)
          edgeRight?.apply(YogaEdge.RIGHT, this, resourceResolver)

          radius?.apply(this, resourceResolver)

          effect?.apply(this)
        }
        .build()

/**
 * Stores properties that are used to customise border edges
 *
 * @param color the colour to be applied to the edge
 * @param width the width of the edge
 */
class BorderEdge(@param:ColorInt val color: Int? = null, val width: Dimen? = null) {

  @PublishedApi
  internal fun apply(
      edge: YogaEdge,
      builder: Border.Builder,
      resourceResolver: ResourceResolver,
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
  internal fun apply(builder: Border.Builder, resourceResolver: ResourceResolver) {
    all?.let { builder.radiusPx(it.toPixels(resourceResolver)) }
    topLeft?.let { builder.radiusPx(Border.Corner.TOP_LEFT, it.toPixels(resourceResolver)) }
    topRight?.let { builder.radiusPx(Border.Corner.TOP_RIGHT, it.toPixels(resourceResolver)) }
    bottomLeft?.let { builder.radiusPx(Border.Corner.BOTTOM_LEFT, it.toPixels(resourceResolver)) }
    bottomRight?.let { builder.radiusPx(Border.Corner.BOTTOM_RIGHT, it.toPixels(resourceResolver)) }
  }
}
