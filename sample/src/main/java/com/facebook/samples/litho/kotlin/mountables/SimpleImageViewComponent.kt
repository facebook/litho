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

package com.facebook.samples.litho.kotlin.mountables

import android.content.Context
import android.widget.ImageView
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableWithStyle
import com.facebook.litho.SimpleMountable
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.RenderState.LayoutContext
import com.facebook.samples.litho.R

// start_simple_mountable_component_example
class SimpleImageViewComponent(private val style: Style? = null) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableWithStyle {
    return MountableWithStyle(SimpleImageViewMountable(), style)
  }
}
// end_simple_mountable_component_example

private const val defaultSize: Int = 150

internal class SimpleImageViewMountable() : SimpleMountable<ImageView>(RenderType.VIEW) {

  override fun createContent(context: Context): ImageView = ImageView(context)

  override fun measure(context: LayoutContext<*>, widthSpec: Int, heightSpec: Int): MeasureResult {
    return if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED &&
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
      MeasureResult(defaultSize, defaultSize)
    } else {
      MeasureResult.withEqualDimensions(widthSpec, heightSpec, null)
    }
  }

  override fun mount(c: Context, content: ImageView, layoutData: Any?) {
    content.setImageDrawable(c.resources.getDrawable(R.drawable.ic_launcher))
  }

  override fun unmount(c: Context, content: ImageView, layoutData: Any?) {
    content.setImageResource(0)
  }
}
