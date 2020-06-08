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

package com.facebook.samples.litho.animations.sharedelements;

import static com.facebook.samples.litho.animations.sharedelements.DetailActivity.INTENT_COLOR_KEY;
import static com.facebook.samples.litho.animations.sharedelements.DetailActivity.INTENT_LAND_LITHO;
import static com.facebook.samples.litho.animations.sharedelements.DetailActivity.SQUARE_TRANSITION_NAME;
import static com.facebook.samples.litho.animations.sharedelements.DetailActivity.TITLE_TRANSITION_NAME;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class SharedElementsComponentSpec {

  private static final String SQUARE_TRANSITION_NAME_NUMBER = "SQUARE_TRANSITION_NAME_NUMBER_";

  private static final int[] COLORS = {
    Color.BLACK,
    Color.DKGRAY,
    Color.GRAY,
    Color.LTGRAY,
    Color.RED,
    Color.GREEN,
    Color.BLUE,
    Color.YELLOW,
    Color.CYAN
  };

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean landLitho) {
    Spannable spannable = new SpannableString("SHARED ELEMENT " + (landLitho ? "LITHO" : "XML"));
    spannable.setSpan(
        new ForegroundColorSpan(Color.RED),
        14,
        spannable.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    Column.Builder columnBuilder =
        Column.create(c)
            .backgroundColor(Color.WHITE)
            .alignItems(YogaAlign.CENTER)
            .child(
                Text.create(c)
                    .text("(click title to change landing activity render from XML to Litho)"))
            .child(
                Text.create(c)
                    .textSizeSp(25)
                    .transitionName(TITLE_TRANSITION_NAME)
                    .text(spannable)
                    .viewTag(TITLE_TRANSITION_NAME)
                    .clickHandler(SharedElementsComponent.onTitleClickEvent(c))
                    .marginDip(YogaEdge.BOTTOM, 5));
    for (int i = 0; i < COLORS.length; i++) {
      columnBuilder.child(getLithoSquare(c, i));
    }
    return columnBuilder.build();
  }

  private static Component.Builder getLithoSquare(ComponentContext c, int index) {
    return Row.create(c)
        .marginDip(YogaEdge.TOP, 5)
        .widthDip(50)
        .heightDip(50)
        .transitionName(SQUARE_TRANSITION_NAME_NUMBER + index)
        .backgroundColor(COLORS[index])
        .clickHandler(SharedElementsComponent.onClickEvent(c, COLORS[index]));
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> landLitho) {
    landLitho.set(!landLitho.get());
  }

  @OnEvent(ClickEvent.class)
  static void onTitleClickEvent(ComponentContext c, @FromEvent View view) {
    SharedElementsComponent.updateState(c);
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(
      ComponentContext c, @FromEvent View view, @Param int color, @State boolean landLitho) {
    Activity activity = (Activity) c.getAndroidContext();
    Intent intent = new Intent(c.getAndroidContext(), DetailActivity.class);
    intent.putExtra(INTENT_COLOR_KEY, color);
    intent.putExtra(INTENT_LAND_LITHO, landLitho);
    ActivityOptionsCompat options =
        ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity,
            new Pair<View, String>(view, SQUARE_TRANSITION_NAME),
            new Pair<View, String>(
                activity.getWindow().getDecorView().findViewWithTag(TITLE_TRANSITION_NAME),
                TITLE_TRANSITION_NAME));
    activity.startActivity(intent, options.toBundle());
  }
}
