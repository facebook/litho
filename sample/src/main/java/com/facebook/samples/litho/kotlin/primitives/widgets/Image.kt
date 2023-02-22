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

package com.facebook.samples.litho.kotlin.primitives.widgets

import android.graphics.drawable.Drawable
import android.widget.ImageView.ScaleType
import com.facebook.litho.DrawableMatrix
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult

/**
 * A component to render a [Drawable].
 *
 * Tip: Use the same instance of the [drawable] to avoid remounting it in subsequent layouts to
 * improve performance.
 *
 * @param drawable The [Drawable] to render.
 * @param scaleType The [ScaleType] to scale or transform the [drawable].
 */
class Image(
    private val drawable: Drawable,
    private val scaleType: ScaleType = ScaleType.FIT_XY,
    private val style: Style? = null
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ImageLayoutBehavior(drawable, scaleType),
        mountBehavior =
            MountBehavior(DrawableAllocator(poolSize = 30) { MatrixDrawable<Drawable>() }) {
              bindWithLayoutData<PrimitiveImageLayoutData>(drawable, scaleType) {
                  content,
                  layoutData ->
                content.mount(drawable, layoutData.matrix)
                content.bind(layoutData.width, layoutData.height)
                onUnbind { content.unmount() }
              }
            },
        style = style)
  }
}

internal class ImageLayoutBehavior(
    private val drawable: Drawable,
    private val scaleType: ScaleType
) : LayoutBehavior {
  override fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult {
    val intrinsicWidth = drawable.intrinsicWidth
    val intrinsicHeight = drawable.intrinsicHeight

    val result =
        if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED &&
            SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
          MeasureResult(intrinsicWidth, intrinsicHeight)
        } else {
          MeasureResult.forAspectRatio(
              widthSpec,
              heightSpec,
              intrinsicWidth,
              intrinsicHeight,
              intrinsicWidth.toFloat() / intrinsicHeight.toFloat())
        }

    val matrix =
        if (scaleType == ScaleType.FIT_XY || intrinsicWidth <= 0 || intrinsicHeight <= 0) {
          null
        } else {
          DrawableMatrix.create(drawable, scaleType, result.width, result.height)
        }

    val useLayoutSize = ScaleType.FIT_XY == scaleType || intrinsicWidth <= 0 || intrinsicHeight <= 0

    return PrimitiveLayoutResult(
        widthSpec = widthSpec,
        heightSpec = heightSpec,
        width = result.width,
        height = result.height,
        layoutData =
            PrimitiveImageLayoutData(
                if (useLayoutSize) result.width else intrinsicWidth,
                if (useLayoutSize) result.height else intrinsicHeight,
                matrix))
  }
}

/**
 * The layout data required by the [Image] Primitive to mount, and bind the drawable in the host.
 */
class PrimitiveImageLayoutData(val width: Int, val height: Int, val matrix: DrawableMatrix? = null)
