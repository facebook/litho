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

package com.facebook.samples.litho.kotlin.sizeconstraintsawarecomponent

import android.view.View.MeasureSpec.UNSPECIFIED
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.SizeConstraintsAwareComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.drawableRes
import com.facebook.rendercore.sp
import com.facebook.samples.litho.R
import com.facebook.yoga.YogaAlign

class SizeConstraintsAwareComponentKComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val width = useState { 50 }

    return Column(alignItems = YogaAlign.CENTER, style = Style.padding(all = 20.dp)) {
      child(
          Row {
            child(
                Text(
                    text = "—",
                    textSize = 30.dp,
                    style =
                        Style.padding(all = 30.dp).onClick {
                          width.update { prevCount -> 0.coerceAtLeast(prevCount - 25) }
                        }))
            child(
                Text(text = "${width.value}", textSize = 30.dp, style = Style.margin(all = 30.dp)))
            child(
                Text(
                    text = "+",
                    textSize = 30.dp,
                    style =
                        Style.padding(all = 30.dp).onClick {
                          width.update { prevCount -> prevCount + 25 }
                        }))
          })
      child(
          // start_sizeconstraintsawarecomponent_example
          SizeConstraintsAwareComponent(style = Style.width(width.value.dp)) { sizeConstraints ->
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

            // Assuming sizeConstraints.hasBoundedWidth == true
            val doesTextFit = textOutputSize.width <= sizeConstraints.maxWidth

            if (doesTextFit) textComponent else imageComponent
          })
      // end_sizeconstraintsawarecomponent_example
    }
  }
}
