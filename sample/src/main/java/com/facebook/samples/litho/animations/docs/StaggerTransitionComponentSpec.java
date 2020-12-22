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

package com.facebook.samples.litho.animations.docs;

import android.graphics.Color;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.SolidColor;
import com.facebook.yoga.YogaAlign;

// start
@LayoutSpec
public class StaggerTransitionComponentSpec {
  private static final String YELLOW_KEY = "YELLOW_KEY";
  private static final String BLUE_KEY = "BLUE_KEY";
  private static final String PURPLE_KEY = "PURPLE_KEY";
  private static final int PURPLE_COLOR = Color.rgb(144, 29, 191);

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean shown) {
    return Row.create(c)
        .heightPercent(100)
        .child(
            Row.create(c)
                .child(
                    SolidColor.create(c)
                        .widthDip(90)
                        .heightDip(40)
                        .transitionKey(YELLOW_KEY)
                        .color(Color.YELLOW))
                .child(
                    SolidColor.create(c)
                        .widthDip(90)
                        .heightDip(40)
                        .transitionKey(BLUE_KEY)
                        .color(Color.BLUE))
                .child(
                    SolidColor.create(c)
                        .widthDip(90)
                        .heightDip(40)
                        .transitionKey(PURPLE_KEY)
                        .color(PURPLE_COLOR)))
        .clickHandler(StaggerTransitionComponent.onClickEvent(c))
        .alignItems(shown ? YogaAlign.FLEX_END : YogaAlign.FLEX_START)
        .build();
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.stagger(
        100,
        Transition.create(YELLOW_KEY).animate(AnimatedProperties.Y),
        Transition.create(BLUE_KEY).animate(AnimatedProperties.Y),
        Transition.create(PURPLE_KEY).animate(AnimatedProperties.Y));
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(ComponentContext c, @FromEvent View view) {
    StaggerTransitionComponent.onUpdateState(c);
  }

  @OnUpdateState
  static void onUpdateState(StateValue<Boolean> shown) {
    shown.set(!shown.get());
  }
}
// end
