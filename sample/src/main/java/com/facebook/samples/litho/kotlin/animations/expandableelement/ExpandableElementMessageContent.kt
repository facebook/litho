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

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sp
import com.facebook.litho.view.background

class ExpandableElementMessageContent(
    private val messageText: String,
    private val messageTextColor: Int,
    private val backgroundColor: Int
) : KComponent() {

  override fun ComponentScope.render(): Component {
    return Row(
        style =
            Style.padding(all = 8.dp)
                .margin(all = 8.dp)
                .background(getMessageBackground(backgroundColor))) {
      child(Text(textSize = 18.sp, textColor = messageTextColor, text = messageText))
    }
  }

  fun getMessageBackground(color: Int): ShapeDrawable {
    val roundedRectShape =
        RoundRectShape(
            floatArrayOf(40f, 40f, 40f, 40f, 40f, 40f, 40f, 40f),
            null,
            floatArrayOf(40f, 40f, 40f, 40f, 40f, 40f, 40f, 40f))
    return ShapeDrawable(roundedRectShape).also { it.paint.color = color }
  }
}
