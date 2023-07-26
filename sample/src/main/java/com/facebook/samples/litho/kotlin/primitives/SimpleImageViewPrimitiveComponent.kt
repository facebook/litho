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

package com.facebook.samples.litho.kotlin.primitives

import android.widget.ImageView
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.drawableRes
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.utils.withEqualDimensions
import com.facebook.samples.litho.R

// start_image_primitive_component_example
class SimpleImageViewPrimitiveComponent(private val style: Style? = null) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(primitive = SimpleImageViewPrimitive, style = style)
  }
}

internal val PrimitiveComponentScope.SimpleImageViewPrimitive
  get() =
      Primitive(
          layoutBehavior = ImageLayoutBehavior,
          mountBehavior =
              MountBehavior(ViewAllocator { context -> ImageView(context) }) {
                bind(R.drawable.ic_launcher) { imageView ->
                  imageView.setImageDrawable(drawableRes(R.drawable.ic_launcher))
                  onUnbind { imageView.setImageResource(0) }
                }
              })
// end_image_primitive_component_example

// start_image_primitive_layout_behavior_example
internal object ImageLayoutBehavior : LayoutBehavior {
  private const val defaultSize: Int = 150

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(
        size =
            if (!sizeConstraints.hasBoundedWidth && !sizeConstraints.hasBoundedHeight) {
              Size(defaultSize, defaultSize)
            } else {
              Size.withEqualDimensions(sizeConstraints)
            })
  }
}
// end_image_primitive_layout_behavior_example
