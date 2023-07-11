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

package com.facebook.samples.litho.kotlin.primitives.bindto

import android.graphics.Color
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.facebook.litho.DynamicValue
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.utils.withEqualDimensions
import com.facebook.samples.litho.R

// start_bindTo_imagecomponent_code
class ImageViewComponent(
    private val rotation: DynamicValue<Float>,
    private val background: DynamicValue<Float>,
    private val scale: DynamicValue<Float>,
    private val style: Style? = null
) : PrimitiveComponent() {
  // end_bindTo_imagecomponent_code
  // start_bindTo_binding_code
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ImageLayoutBehavior,
        mountBehavior =
            MountBehavior(ViewAllocator { context -> ImageView(context) }) {
              bind(R.drawable.ic_launcher) { imageView ->
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(context.androidContext, R.drawable.ic_launcher))
                onUnbind { imageView.setImageResource(0) }
              }

              // simple binding
              bindDynamic(rotation, ImageView::setRotation, 0f)
              bindDynamic(scale, ImageView::setScaleX, 1f)
              bindDynamic(scale, ImageView::setScaleY, 1f)

              // complex binding
              bindDynamic(background) { imageView: ImageView, value ->
                imageView.setBackgroundColor(
                    Color.HSVToColor(floatArrayOf(evaluate(value, 0f, 360f), 1f, 1f)))

                onUnbindDynamic { imageView.setBackgroundColor(Color.BLACK) }
              }
            },
        style)
  }
  // end_bindTo_binding_code
}

internal object ImageLayoutBehavior : LayoutBehavior {
  private const val defaultSize: Int = 150

  // start_primitive_measure_example
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(
        size =
            if (!sizeConstraints.hasBoundedWidth && !sizeConstraints.hasBoundedHeight) {
              Size(defaultSize, defaultSize)
            } else {
              Size.withEqualDimensions(sizeConstraints)
            })
  }
  // end_primitive_measure_example
}

fun evaluate(fraction: Float, start: Float, end: Float): Float = start + fraction * (end - start)
