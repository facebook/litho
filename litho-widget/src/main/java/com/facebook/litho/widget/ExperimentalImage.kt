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

package com.facebook.litho.widget

import android.graphics.drawable.Drawable
import android.widget.ImageView.ScaleType
import com.facebook.litho.DrawableMatrix
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.utils.exact
import com.facebook.rendercore.utils.withAspectRatio
import kotlin.math.max

/**
 * A component to render a [Drawable].
 *
 * Tip: Use the same instance of the [drawable] to avoid remounting it in subsequent layouts to
 * improve performance.
 *
 * @param drawable The [Drawable] to render.
 * @param scaleType The [ScaleType] to scale or transform the [drawable].
 */
@ExperimentalLithoApi
class ExperimentalImage(
    private val drawable: Drawable?,
    private val scaleType: ScaleType = ScaleType.FIT_CENTER,
    private val style: Style? = null
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ImageLayoutBehavior(drawable, scaleType),
        mountBehavior =
            // start_image_primitive_mount_behavior_example
            MountBehavior(
                DrawableAllocator(poolSize = 30, canPreallocate = true) {
                  MatrixDrawable<Drawable>()
                }) {
                  bindWithLayoutData<PrimitiveImageLayoutData>(drawable, scaleType) {
                      content,
                      layoutData ->
                    content.mount(drawable, layoutData.matrix)
                    content.bind(layoutData.width, layoutData.height)
                    onUnbind { content.unmount() }
                  }
                }
        // end_image_primitive_mount_behavior_example
        ,
        style = style)
  }
}

internal class ImageLayoutBehavior(
    private val drawable: Drawable?,
    private val scaleType: ScaleType,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val exactSize = Size.exact(sizeConstraints)
    // return 0 size when drawable is null or its intrinsic size is <=0 but size constraints are not
    // exact, but when exact size constraints exist we want to respect them
    if (drawable == null ||
        (exactSize == Size.Invalid &&
            (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0))) {
      return PrimitiveLayoutResult(
          size = Size(0, 0), layoutData = PrimitiveImageLayoutData(0, 0, null))
    }

    val intrinsicWidth = drawable.intrinsicWidth
    val intrinsicHeight = drawable.intrinsicHeight

    val computedSize =
        if (exactSize != Size.Invalid) {
          exactSize
        } else if (!sizeConstraints.hasBoundedWidth && !sizeConstraints.hasBoundedHeight) {
          Size(intrinsicWidth, intrinsicHeight)
        } else {
          Size.withAspectRatio(
              sizeConstraints,
              intrinsicWidth.toFloat() / intrinsicHeight.toFloat(),
              intrinsicWidth,
              intrinsicHeight)
        }

    val size = Size(max(0, computedSize.width), max(0, computedSize.height))

    val matrix =
        if (scaleType == ScaleType.FIT_XY || intrinsicWidth <= 0 || intrinsicHeight <= 0) {
          null
        } else {
          DrawableMatrix.create(drawable, scaleType, size.width, size.height)
        }

    val useLayoutSize = ScaleType.FIT_XY == scaleType || intrinsicWidth <= 0 || intrinsicHeight <= 0

    return PrimitiveLayoutResult(
        size = size,
        layoutData =
            PrimitiveImageLayoutData(
                if (useLayoutSize) size.width else intrinsicWidth,
                if (useLayoutSize) size.height else intrinsicHeight,
                matrix))
  }
}

/**
 * The layout data required by the [Image] Primitive to mount, and bind the drawable in the host.
 */
class PrimitiveImageLayoutData(val width: Int, val height: Int, val matrix: DrawableMatrix? = null)
