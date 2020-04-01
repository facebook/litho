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

import com.facebook.litho.Decoration
import com.facebook.litho.Dp
import com.facebook.litho.FixedSize
import com.facebook.litho.Flex
import com.facebook.litho.KComponent
import com.facebook.litho.Margin
import com.facebook.litho.Padding
import com.facebook.litho.Row
import com.facebook.litho.dp
import com.facebook.litho.drawableColor
import com.facebook.litho.sp
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.lithography.data.Decade
import com.facebook.yoga.YogaAlign.CENTER

class DecadeSeparator(decade: Decade) : KComponent({
  Padding(all = 16.dp) {
    Decoration(background = drawableColor(0xFFFAFAFA)) {
      Row(alignItems = CENTER) {
        +Flex(grow = 1f) {
          FixedSize(height = Dp.Hairline) {
            Decoration(background = drawableColor(0xFFAAAAAA)) {
              Row()
            }
          }
        }
        +Flex(shrink = 0f) {
          Margin(horizontal = 10.dp) {
            Text(text = "${decade.year}", textSize = 14.sp, textColor = 0xFFAAAAAA.toInt())
          }
        }
        +Flex(grow = 1f) {
          FixedSize(height = Dp.Hairline) {
            Decoration(background = drawableColor(0xFFAAAAAA)) {
              Row()
            }
          }
        }
      }
    }
  }
})
