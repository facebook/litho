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

package com.facebook.samples.lithocodelab.examples.modules;

import static android.widget.Toast.LENGTH_SHORT;
import static com.facebook.yoga.YogaAlign.CENTER;
import static com.facebook.yoga.YogaAlign.FLEX_END;
import static com.facebook.yoga.YogaAlign.STRETCH;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;

import android.widget.Toast;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.Text;
import com.facebook.samples.lithocodelab.R;

/** Learn how to handle clicks in Components. Turns out, they're just like @Props on Components */
@LayoutSpec
public class LearningClickEventsComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .alignItems(CENTER)
        .child(
            Text.create(c)
                .text("First child")
                .textSizeDip(50)
                .clickHandler(LearningClickEventsComponent.onClickFirstChild(c))
                .backgroundRes(android.R.color.holo_blue_light)
                .alignSelf(STRETCH)
                .paddingDip(BOTTOM, 20)
                .paddingDip(TOP, 40))
        .child(
            Text.create(c)
                .text("Second child")
                .textColorRes(android.R.color.holo_green_dark)
                .textSizeSp(30)
                .clickHandler(LearningClickEventsComponent.onClickSecondChild(c))
                .backgroundRes(android.R.color.holo_red_light)
                .alignSelf(FLEX_END)
                .marginPercent(RIGHT, 10))
        .child(
            Row.create(c)
                .clickHandler(LearningClickEventsComponent.onClickThirdChild(c))
                .backgroundRes(android.R.color.holo_blue_light)
                .child(
                    Image.create(c)
                        .drawableRes(R.drawable.save)
                        .widthDip(40)
                        .heightDip(40)
                        .paddingDip(START, 7)
                        .paddingDip(END, 7))
                .child(Text.create(c).text("Third child").textSizeSp(30)))
        .child(
            Text.create(c)
                .text("Absolutely positioned child")
                .textColorRes(android.R.color.holo_orange_dark)
                .textSizeSp(15)
                .clickHandler(
                    LearningClickEventsComponent.onClickAbsoluteChild(
                        c, "Param passed in on click."))
                .backgroundRes(android.R.color.holo_purple)
                .positionType(ABSOLUTE)
                .positionDip(START, 10)
                .positionDip(TOP, 10))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickFirstChild(ComponentContext c) {
    Toast.makeText(c.getAndroidContext(), "First child clicked!", LENGTH_SHORT).show();
  }

  @OnEvent(ClickEvent.class)
  static void onClickSecondChild(ComponentContext c, @Prop String secondChildString) {
    Toast.makeText(
            c.getAndroidContext(), "Second child clicked: " + secondChildString, LENGTH_SHORT)
        .show();
  }

  @OnEvent(ClickEvent.class)
  static void onClickThirdChild(ComponentContext c) {
    Toast.makeText(c.getAndroidContext(), "Third child clicked!", LENGTH_SHORT).show();
  }

  @OnEvent(ClickEvent.class)
  static void onClickAbsoluteChild(ComponentContext c, @Param String absoluteParam) {
    Toast.makeText(c.getAndroidContext(), "Absolute child clicked: " + absoluteParam, LENGTH_SHORT)
        .show();
  }
}
