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

package com.facebook.samples.lithocodelab.end;

import static com.facebook.yoga.YogaAlign.STRETCH;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaJustify.CENTER;

import android.graphics.Color;
import com.facebook.litho.Border;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.Text;
import com.facebook.samples.lithocodelab.R;
import com.facebook.yoga.YogaAlign;

/**
 * Renders a "story card" with a header and message. This also has a togglable "saved" state.
 *
 * <p>This and the header do most of the interesting stuff for the approximate end state for the lab
 * activity.
 */
@LayoutSpec
class StoryCardComponentSpec {

  static final float CARD_INSET = 12.0f;
  static final float CARD_INTERNAL_PADDING = 7.0f;

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop Component header, @Prop String content, @State Boolean saved) {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .child(header)
        .child(
            Text.create(c, 0, R.style.message_text)
                .text(content)
                .paddingDip(HORIZONTAL, CARD_INSET)
                .paddingDip(BOTTOM, CARD_INTERNAL_PADDING))
        .child(
            Row.create(c)
                .backgroundColor(saved ? Color.BLUE : Color.TRANSPARENT)
                .alignSelf(STRETCH)
                .paddingDip(HORIZONTAL, CARD_INSET)
                .paddingDip(BOTTOM, CARD_INTERNAL_PADDING)
                .paddingDip(TOP, CARD_INTERNAL_PADDING)
                .justifyContent(CENTER)
                .child(
                    Image.create(c)
                        .drawableRes(R.drawable.save)
                        .alignSelf(YogaAlign.CENTER)
                        .widthDip(20)
                        .heightDip(20)
                        .marginDip(END, CARD_INTERNAL_PADDING))
                .child(Text.create(c, 0, R.style.save_text).text("Save"))
                .clickHandler(StoryCardComponent.onClickSave(c))
                .border(Border.create(c).color(ALL, Color.BLACK).widthDip(TOP, 1).build()))
        .border(Border.create(c).color(ALL, Color.BLACK).widthDip(ALL, 1).build())
        .build();
  }

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Boolean> saved) {
    saved.set(false);
  }

  @OnUpdateState
  static void onToggleSavedState(StateValue<Boolean> saved) {
    saved.set(!saved.get());
  }

  @OnEvent(ClickEvent.class)
  static void onClickSave(ComponentContext c) {
    StoryCardComponent.onToggleSavedStateSync(c);
  }
}
