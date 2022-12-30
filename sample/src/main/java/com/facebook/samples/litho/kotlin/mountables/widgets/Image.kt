// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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

package com.facebook.samples.litho.kotlin.mountables.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView.ScaleType
import com.facebook.litho.DrawableMatrix
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.MeasureScope
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.SizeSpec
import com.facebook.litho.SizeSpec.UNSPECIFIED
import com.facebook.litho.Style
import com.facebook.litho.drawable.DrawableUtils
import com.facebook.rendercore.MeasureResult

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
    val drawable: Drawable,
    val scaleType: ScaleType? = ScaleType.FIT_XY,
    val style: Style? = null,
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult =
      MountableRenderResult(ImageMountable(drawable, scaleType ?: ScaleType.FIT_XY), style)
}

/**
 * The [SimpleMountable] used by the [Image]. It uses a [MatrixDrawable], and [DrawableMatrix] to
 * correctly render the actual [drawable] inside it.
 */
internal class ImageMountable(
    val drawable: Drawable,
    val scaleType: ScaleType,
) : SimpleMountable<MatrixDrawable<Drawable>>(RenderType.DRAWABLE) {

  override fun createContent(context: Context): MatrixDrawable<Drawable> {
    return MatrixDrawable()
  }

  override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult {
    val intrinsicWidth = drawable.intrinsicWidth
    val intrinsicHeight = drawable.intrinsicHeight

    val result =
        if (SizeSpec.getMode(widthSpec) == UNSPECIFIED &&
            SizeSpec.getMode(heightSpec) == UNSPECIFIED) {
          MeasureResult(intrinsicWidth, intrinsicHeight)
        } else {
          forAspectRatio(
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

    return MeasureResult(
        result.width,
        result.height,
        ImageLayoutData(
            if (useLayoutSize) result.width else intrinsicWidth,
            if (useLayoutSize) result.height else intrinsicHeight,
            matrix))
  }

  override fun mount(c: Context, content: MatrixDrawable<Drawable>, layoutData: Any?) {
    layoutData as ImageLayoutData
    content.mount(drawable, layoutData.matrix)
    content.bind(layoutData.width, layoutData.height)
  }

  override fun unmount(c: Context, content: MatrixDrawable<Drawable>, layoutData: Any?) {
    content.unmount()
  }

  override fun shouldUpdate(
      newMountable: SimpleMountable<MatrixDrawable<Drawable>>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    newMountable as ImageMountable
    return (newMountable.scaleType != scaleType ||
        !DrawableUtils.isEquivalentTo(newMountable.drawable, drawable))
  }

  override fun poolSize(): Int = 30
}

/** The layout data required by the [ImageMountable] to mount, and bind the drawable in the host. */
class ImageLayoutData(val width: Int, val height: Int, val matrix: DrawableMatrix? = null)
