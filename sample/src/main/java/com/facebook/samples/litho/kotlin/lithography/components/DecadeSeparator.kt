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

package com.facebook.samples.litho.kotlin.lithography.components

import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.drawableColor
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.px
import com.facebook.litho.sp
import com.facebook.litho.view.background
import com.facebook.samples.litho.kotlin.lithography.data.Decade
import com.facebook.yoga.YogaAlign.CENTER

class DecadeSeparator(val decade: Decade) : KComponent() {
  override fun ComponentScope.render() =
      Row(alignItems = CENTER, style = Style.padding(16.dp).background(drawableColor(0xFFFAFAFA))) {
        child(Row(style = Style.height(1.px).flex(grow = 1f).background(drawableColor(0xFFAAAAAA))))
        child(
            Text(
                text = "${decade.year}",
                textSize = 14.sp,
                textColor = 0xFFAAAAAA.toInt(),
                style = Style.margin(horizontal = 10.dp).flex(shrink = 0f)))
        child(Row(style = Style.height(1.px).flex(grow = 1f).background(drawableColor(0xFFAAAAAA))))
      }
}
