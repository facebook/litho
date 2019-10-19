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

package com.facebook.samples.litho.animations.animationcomposition;

import android.graphics.Color;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
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
public class LeftRightBlocksComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean left) {
    return Column.create(c)
        .alignItems(left ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#ee1111"))
                .transitionKey("red")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#1111ee"))
                .transitionKey("blue")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#11ee11"))
                .transitionKey("green")
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.BLACK)
                .transitionKey("black")
                .build())
        .clickHandler(LeftRightBlocksComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    LeftRightBlocksComponent.updateStateSync(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> left) {
    left.set(left.get() == true ? false : true);
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.parallel(
        Transition.create("red")
            .animate(AnimatedProperties.X)
            .animator(Transition.timing(1000, new LinearInterpolator())),
        Transition.create("blue")
            .animate(AnimatedProperties.X)
            .animator(
                Transition.timing(
                    1000)), // uses default interpolator AccelerateDecelerateInterpolator
        Transition.create("green")
            .animate(AnimatedProperties.X)
            .animator(Transition.timing(1000, new BounceInterpolator())),
        Transition.delay(
            1000,
            Transition.create("black")
                .animate(AnimatedProperties.X)
                .animator(Transition.timing(1000))));
  }
}
