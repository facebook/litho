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

import android.graphics.Typeface
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.drawableRes
import com.facebook.litho.flexbox.aspectRatio
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.yoga.YogaAlign

// start_example
class FlexboxComponent(
    private val username: String,
    @DrawableRes private val avatarRes: Int,
    @DrawableRes private val imageRes: Int
) : KComponent() {
  override fun ComponentScope.render(): Component? {
    return Column {
      child(
          Row(alignItems = YogaAlign.CENTER, style = Style.padding(all = 8.dp)) {
            child(
                Image(
                    drawable = drawableRes(avatarRes),
                    style = Style.width(36.dp).height(36.dp).margin(start = 4.dp, end = 8.dp)))
            child(Text(text = username, textStyle = Typeface.BOLD))
          })
      child(
          Image(
              drawable = drawableRes(imageRes),
              scaleType = ImageView.ScaleType.CENTER_CROP,
              style = Style.aspectRatio(1f)))
    }
  }
}
// end_example
