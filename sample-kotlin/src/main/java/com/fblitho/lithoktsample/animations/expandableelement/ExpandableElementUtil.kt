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

package com.fblitho.lithoktsample.animations.expandableelement

import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Transition
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge

object ExpandableElementUtil {
  const val TRANSITION_MSG_PARENT = "transition_msg_parent"
  const val TRANSITION_TEXT_MESSAGE_WITH_BOTTOM = "transition_text_msg_with_bottom"
  const val TRANSITION_TOP_DETAIL = "transition_top_detail"
  const val TRANSITION_BOTTOM_DETAIL = "transition_bottom_detail"

  fun getMessageBackground(c: ComponentContext, color: Int): ShapeDrawable {
    val roundedRectShape = RoundRectShape(
        floatArrayOf(40f, 40f, 40f, 40f, 40f, 40f, 40f, 40f),
        null,
        floatArrayOf(40f, 40f, 40f, 40f, 40f, 40f, 40f, 40f))
    return ShapeDrawable(roundedRectShape)
        .also {
          it.paint.color = color
        }
  }

  fun maybeCreateBottomDetailComponent(
      c: ComponentContext,
      expanded: Boolean,
      seen: Boolean
  ): Component.Builder<*>? = if (!expanded) {
    null
  } else {
    Text.create(c)
        .textSizeDip(14f)
        .textColor(Color.GRAY)
        .alignSelf(YogaAlign.FLEX_END)
        .paddingDip(YogaEdge.RIGHT, 10f)
        .transitionKey(TRANSITION_BOTTOM_DETAIL)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .text(if (seen) "Seen" else "Sent")
  }

  fun maybeCreateTopDetailComponent(
      c: ComponentContext,
      expanded: Boolean,
      timestamp: String
  ): Component.Builder<*>? = if (!expanded) {
    null
  } else {
    Text.create(c)
        .textSizeDip(14f)
        .textColor(Color.GRAY)
        .alignSelf(YogaAlign.CENTER)
        .transitionKey(TRANSITION_TOP_DETAIL)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .text(timestamp)
  }
}
