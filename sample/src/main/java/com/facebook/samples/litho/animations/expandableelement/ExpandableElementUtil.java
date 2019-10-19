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

package com.facebook.samples.litho.animations.expandableelement;

import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Transition;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

class ExpandableElementUtil {
  static final String TRANSITION_MSG_PARENT = "transition_msg_parent";
  static final String TRANSITION_TEXT_MESSAGE_WITH_BOTTOM = "transition_text_msg_with_bottom";
  static final String TRANSITION_TOP_DETAIL = "transition_top_detail";
  static final String TRANSITION_BOTTOM_DETAIL = "transition_bottom_detail";

  static ShapeDrawable getMessageBackground(ComponentContext c, int color) {
    final RoundRectShape roundedRectShape =
        new RoundRectShape(
            new float[] {40, 40, 40, 40, 40, 40, 40, 40},
            null,
            new float[] {40, 40, 40, 40, 40, 40, 40, 40});
    final ShapeDrawable oval = new ShapeDrawable(roundedRectShape);
    oval.getPaint().setColor(color);
    return oval;
  }

  @Nullable
  static Component.Builder maybeCreateBottomDetailComponent(
      ComponentContext c, boolean expanded, boolean seen) {
    if (!expanded) {
      return null;
    }

    return Text.create(c)
        .textSizeDip(14)
        .textColor(Color.GRAY)
        .alignSelf(YogaAlign.FLEX_END)
        .paddingDip(YogaEdge.RIGHT, 10)
        .transitionKey(TRANSITION_BOTTOM_DETAIL)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .text(seen ? "Seen" : "Sent");
  }

  @Nullable
  static Component.Builder maybeCreateTopDetailComponent(
      ComponentContext c, boolean expanded, String timestamp) {
    if (!expanded) {
      return null;
    }

    return Text.create(c)
        .textSizeDip(14)
        .textColor(Color.GRAY)
        .alignSelf(YogaAlign.CENTER)
        .transitionKey(TRANSITION_TOP_DETAIL)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .text(timestamp);
  }
}
