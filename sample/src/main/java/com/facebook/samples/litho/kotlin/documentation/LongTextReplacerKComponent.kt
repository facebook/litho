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

package com.facebook.samples.litho.kotlin.documentation

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.SizeSpec.UNSPECIFIED
import com.facebook.litho.drawableRes
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sp
import com.facebook.samples.litho.R

// start_example
class LongTextReplacerKComponent(private val widthSpec: Int, private val heightSpec: Int) :
    KComponent() {

  override fun ComponentScope.render(): Component {
    val textComponent = Text(textSize = 16.sp, text = "Some text to measure")

    val textOutputSize = Size()

    textComponent.measure(
        context,
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        textOutputSize)

    // Small component to use in case textComponent doesn’t fit within
    // the current layout.
    val imageComponent = Image(drawable = drawableRes(R.drawable.ic_launcher))

    // Assuming SizeSpec.getMode(widthSpec) == EXACTLY or AT_MOST.
    val layoutWidth = SizeSpec.getSize(widthSpec)
    val textFits = textOutputSize.width <= layoutWidth

    return if (textFits) textComponent else imageComponent
  }
}
// end_example
