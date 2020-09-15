/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.samples.litho.kotlin.lithography.components

import com.facebook.litho.Dp
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.background
import com.facebook.litho.dp
import com.facebook.litho.drawableColor
import com.facebook.litho.flex
import com.facebook.litho.margin
import com.facebook.litho.padding
import com.facebook.litho.size
import com.facebook.litho.sp
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.lithography.data.Decade
import com.facebook.yoga.YogaAlign.CENTER

class DecadeSeparator(decade: Decade) : KComponent({
  Row(alignItems = CENTER, style = Style
      .padding(16.dp)
      .background(drawableColor(0xFFFAFAFA))) {

    +Row(style = Style
        .size(height = Dp.Hairline)
        .flex(grow = 1f)
        .background(drawableColor(0xFFAAAAAA)))

    +Text(
        text = "${decade.year}",
        textSize = 14.sp,
        textColor = 0xFFAAAAAA.toInt(),
        style = Style
            .margin(horizontal = 10.dp)
            .flex(shrink = 0f))

    +Row(style = Style
        .size(height = Dp.Hairline)
        .flex(grow = 1f)
        .background(drawableColor(0xFFAAAAAA)))
  }
})
