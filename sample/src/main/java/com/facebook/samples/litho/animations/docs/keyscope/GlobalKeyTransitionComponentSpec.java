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

package com.facebook.samples.litho.animations.docs.keyscope;

import static android.graphics.Color.YELLOW;

import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.SolidColor;
import com.facebook.yoga.YogaAlign;

// start_working
@LayoutSpec
public class GlobalKeyTransitionComponentSpec {
  public static final String SQUARE_KEY = "square";

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean toRight) {
    return Column.create(c)
        .child(
            SolidColor.create(c)
                .color(YELLOW)
                .widthDip(80)
                .heightDip(80)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .transitionKey(SQUARE_KEY))
        .alignItems(toRight ? YogaAlign.FLEX_END : YogaAlign.FLEX_START)
        .clickHandler(GlobalKeyTransitionComponent.onClickEvent(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(ComponentContext c, @FromEvent View view) {
    GlobalKeyTransitionComponent.onUpdateState(c);
  }

  @OnUpdateState
  static void onUpdateState(StateValue<Boolean> toRight) {
    toRight.set(!toRight.get());
  }
}
// end_working

/*
// not_working_start
@LayoutSpec
public class GlobalKeyTransitionComponentSpec {
  public static final String SQUARE_KEY = "square";

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean toRight) {
    return Column.create(c)
        .child(
            SolidColor.create(c)
                .color(YELLOW)
                .widthDip(80)
                .heightDip(80)
                .transitionKey(SQUARE_KEY))
        .alignItems(toRight ? YogaAlign.FLEX_END : YogaAlign.FLEX_START)
        .clickHandler(GlobalKeyTransitionComponent.onClickEvent(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(ComponentContext c, @FromEvent View view) {
    GlobalKeyTransitionComponent.onUpdateState(c);
  }

  @OnUpdateState
  static void onUpdateState(StateValue<Boolean> toRight) {
    toRight.set(!toRight.get());
  }
}
// not_working_end
 */
