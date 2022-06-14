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

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView.ScaleType
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.DrawableMatrix
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.MeasureResult
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableWithStyle
import com.facebook.litho.SimpleMountable
import com.facebook.litho.SizeSpec
import com.facebook.litho.SizeSpec.EXACTLY
import com.facebook.litho.Style
import com.facebook.litho.drawable.DrawableUtils
import com.facebook.litho.utils.MeasureUtils
import com.facebook.rendercore.RenderUnit

/**
 * A component to render a [Drawable].
 *
 * Tip: Use the same instance of the [drawable] to avoid remounting it in subsequent layouts to
 * improve performance.
 *
 * @param drawable The [Drawable] to render.
 * @param scaleType The [ScaleType] to scale or transform the [drawable].
 */
class ExperimentalImage(
    val drawable: Drawable,
    val scaleType: ScaleType? = ScaleType.FIT_XY,
    val style: Style? = null,
) : MountableComponent() {

  override fun ComponentScope.render(): MountableWithStyle =
      MountableWithStyle(ImageMountable(drawable, scaleType ?: ScaleType.FIT_XY), style)
}

/**
 * The [SimpleMountable] used by the [ExperimentalImage]. It uses a [MatrixDrawable], and
 * [DrawableMatrix] to correctly render the actual [drawable] inside it.
 */
internal class ImageMountable(
    val drawable: Drawable,
    val scaleType: ScaleType,
) : SimpleMountable<MatrixDrawable<Drawable>>() {

  override fun createContent(context: Context): MatrixDrawable<Drawable> {
    return MatrixDrawable()
  }

  override fun measure(
      context: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      previousLayoutData: Any?
  ): MeasureResult {

    val width =
        if (SizeSpec.getMode(widthSpec) == EXACTLY ||
            drawable.intrinsicWidth <= 0 ||
            scaleType == ScaleType.FIT_XY) {
          SizeSpec.getSize(widthSpec)
        } else {
          drawable.intrinsicWidth
        }

    val height =
        if (SizeSpec.getMode(heightSpec) == EXACTLY ||
            drawable.intrinsicHeight <= 0 ||
            scaleType == ScaleType.FIT_XY) {
          SizeSpec.getSize(heightSpec)
        } else {
          drawable.intrinsicHeight
        }

    val matrix =
        if (scaleType == ScaleType.FIT_XY ||
            drawable.intrinsicWidth <= 0 ||
            drawable.intrinsicHeight <= 0) {
          null
        } else {
          DrawableMatrix.create(
              drawable,
              scaleType,
              width,
              height,
          )
        }

    return MeasureUtils.measureResultUsingAspectRatio(
        widthSpec,
        heightSpec,
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        (drawable.intrinsicWidth / drawable.intrinsicHeight).toFloat(),
        ImageLayoutData(
            if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else (width),
            if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else (height),
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
      currentMountable: SimpleMountable<MatrixDrawable<Drawable>>,
      newMountable: SimpleMountable<MatrixDrawable<Drawable>>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    currentMountable as ImageMountable
    newMountable as ImageMountable
    return (newMountable.scaleType != currentMountable.scaleType ||
        !DrawableUtils.isEquivalentTo(newMountable.drawable, currentMountable.drawable))
  }

  override fun getRenderType(): RenderUnit.RenderType = RenderUnit.RenderType.DRAWABLE

  override fun getPoolSize(): Int = 30
}

/** The layout data required by the [ImageMountable] to mount, and bind the drawable in the host. */
class ImageLayoutData(val width: Int, val height: Int, val matrix: DrawableMatrix? = null)
