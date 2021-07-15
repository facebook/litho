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

package com.facebook.samples.litho.java.animations.animationcomposition;

import android.graphics.Color;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaAlign;

@LayoutSpec
public class UpDownBlocksComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean top) {
    return Row.create(c)
        .heightDip(200)
        .alignItems(top ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
        .child(
            Row.create(c)
                .heightDip(40)
                .flexGrow(1)
                .backgroundColor(Color.parseColor("#ee1111"))
                .transitionKey("red")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .flexGrow(1)
                .backgroundColor(Color.parseColor("#1111ee"))
                .transitionKey("blue")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .flexGrow(1)
                .backgroundColor(Color.parseColor("#11ee11"))
                .transitionKey("green")
                .build())
        .clickHandler(UpDownBlocksComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    UpDownBlocksComponent.updateStateSync(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> top) {
    top.set(!top.get());
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.stagger(
        200,
        Transition.create("red").animate(AnimatedProperties.Y),
        Transition.create("blue").animate(AnimatedProperties.Y),
        Transition.create("green").animate(AnimatedProperties.Y));
  }
}
