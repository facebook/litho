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

package com.facebook.samples.litho.kotlin.animations.expandableelement

import ExpandableElementMessageContent
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.flex
import com.facebook.litho.view.background
import com.facebook.yoga.YogaAlign

class ExpandableElementOther(
    private val messageText: String,
    private val timestamp: String,
    private val seen: Boolean = false
) : KComponent() {

  override fun ComponentScope.render(): Component? {
    return ExpandableElement(
        Row(style = Style.padding(end = 5.dp)) {
          child(getSenderTitle())
          child(
              ExpandableElementMessageContent(
                  backgroundColor = 0xFFEAEAEA.toInt(),
                  messageTextColor = Color.BLACK,
                  messageText = messageText))
        },
        timestamp = timestamp,
        seen = seen)
  }

  private fun ResourcesScope.getSenderTitle(): Component =
      Row(
          style =
              Style.margin(all = 5.dp)
                  .alignSelf(YogaAlign.CENTER)
                  .width(55.dp)
                  .height(55.dp)
                  .flex(shrink = 0f)
                  .background(getCircle()))

  private fun getCircle(): ShapeDrawable =
      ShapeDrawable(OvalShape()).also { it.paint.color = Color.LTGRAY }
}
