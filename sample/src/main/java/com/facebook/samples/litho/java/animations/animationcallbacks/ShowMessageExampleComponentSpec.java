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

package com.facebook.samples.litho.java.animations.animationcallbacks;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.TransitionEndEvent;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import java.util.Arrays;

@LayoutSpec
public class ShowMessageExampleComponentSpec {
  private static final int START_RADIOUS = 50;
  private static final int END_RADIOUS = 150;
  private static final String CIRCLE_TRANSITION_KEY = "CIRCLE";
  private static final Transition.TransitionAnimator ANIMATOR = Transition.springWithConfig(100, 6);

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c, StateValue<Boolean> radiusStart, StateValue<Boolean> isRunning) {
    radiusStart.set(true);
    isRunning.set(false);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @State boolean radiusStart, @State boolean isRunning) {
    final int radius = radiusStart ? START_RADIOUS : END_RADIOUS;
    final int dim = 2 * radius;
    Column.Builder columnBuilder =
        Column.create(c)
            .child(
                Row.create(c)
                    .heightPx(dim)
                    .widthPx(dim)
                    .transitionKey(CIRCLE_TRANSITION_KEY)
                    .clickHandler(ShowMessageExampleComponent.onClick(c))
                    .background(buildRoundedRect(radius)));
    if (!isRunning) {
      columnBuilder.child(Text.create(c).text("Finish"));
    }
    return columnBuilder.build();
  }

  private static Drawable buildRoundedRect(int radius) {
    final float[] radii = new float[8];
    Arrays.fill(radii, radius);
    final RoundRectShape roundedRectShape = new RoundRectShape(radii, null, radii);
    final ShapeDrawable drawable = new ShapeDrawable(roundedRectShape);
    drawable.getPaint().setColor(Color.RED);
    return drawable;
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    ShowMessageExampleComponent.updateState(c);
  }

  @OnEvent(TransitionEndEvent.class)
  static void onTransitionEnd(ComponentContext c) {
    ShowMessageExampleComponent.updateAnimationStatus(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> radiusStart, StateValue<Boolean> isRunning) {
    radiusStart.set(!radiusStart.get());
    isRunning.set(true);
  }

  @OnUpdateState
  static void updateAnimationStatus(
      StateValue<Boolean> radiusStart, StateValue<Boolean> isRunning) {
    isRunning.set(false);
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.create(CIRCLE_TRANSITION_KEY)
        .animate(
            AnimatedProperties.X,
            AnimatedProperties.Y,
            AnimatedProperties.HEIGHT,
            AnimatedProperties.WIDTH)
        .animator(ANIMATOR)
        .transitionEndHandler(ShowMessageExampleComponent.onTransitionEnd(c));
  }
}
