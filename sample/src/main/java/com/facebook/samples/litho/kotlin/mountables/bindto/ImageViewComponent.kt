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

package com.facebook.samples.litho.kotlin.mountables.bindto

import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import com.facebook.litho.DynamicValue
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.RenderState.LayoutContext
import com.facebook.samples.litho.R
import com.facebook.samples.litho.R.drawable.ic_launcher

// start_bindTo_imagecomponent_code
class ImageViewComponent(
    private val rotation: DynamicValue<Float>,
    private val background: DynamicValue<Float>,
    private val scale: DynamicValue<Float>,
    private val style: Style? = null
) : MountableComponent() {
  // end_bindTo_imagecomponent_code

  override fun MountableComponentScope.render(): MountableRenderResult {
    // start_bindTo_binding_code
    // simple binding
    rotation.bindTo(0f, ImageView::setRotation)
    scale.bindTo(1f, ImageView::setScaleX)
    scale.bindTo(1f, ImageView::setScaleY)

    // complex binding
    background.bindTo(0f) { view: ImageView, value ->
      view.setBackgroundColor(Color.HSVToColor(floatArrayOf(evaluate(value, 0f, 360f), 1f, 1f)))
    }

    // end_bindTo_binding_code

    return MountableRenderResult(ImageViewMountable(), style)
  }
}

private const val defaultSize: Int = 150

internal class ImageViewMountable() : SimpleMountable<ImageView>(RenderType.VIEW) {

  // create_content_example_start
  override fun createContent(context: Context): ImageView = ImageView(context)
  // create_content_example_end

  // measure_example_start
  override fun measure(
      context: LayoutContext<*>,
      widthSpec: Int,
      heightSpec: Int,
      previousLayoutData: Any?
  ): MeasureResult {
    return if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED &&
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
      MeasureResult(defaultSize, defaultSize)
    } else {
      MeasureResult.withEqualDimensions(widthSpec, heightSpec, null)
    }
  }
  // measure_example_end

  // mount_unmount_example_start
  override fun mount(c: Context, content: ImageView, layoutData: Any?) {
    content.setImageDrawable(c.getResources().getDrawable(R.drawable.ic_launcher))
  }

  override fun unmount(c: Context, content: ImageView, layoutData: Any?) {
    content.setImageResource(0)
    content.rotation = 0f
    content.scaleX = 1f
    content.scaleY = 1f
    content.setBackgroundColor(Color.BLACK)
  }
  // mount_unmount_example_end
}

fun evaluate(fraction: Float, start: Float, end: Float): Float = start + fraction * (end - start)
