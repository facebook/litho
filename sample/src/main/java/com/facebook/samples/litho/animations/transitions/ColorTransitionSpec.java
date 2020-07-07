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

package com.facebook.samples.litho.animations.transitions;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.graphics.Color;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Dimension;
import com.facebook.litho.Animations;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaEdge;
import java.util.concurrent.atomic.AtomicReference;

@LayoutSpec
public class ColorTransitionSpec {

  private static final long ANIMATION_DURATION_MS = 300;
  private static final @Dimension(unit = Dimension.DP) float SIZE_DP = 50;

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<AtomicReference<Animator>> animatorRef,
      StateValue<DynamicValue<Float>> animatedValue,
      StateValue<AtomicReference<Boolean>> isCompleteRef,
      StateValue<DynamicValue<Integer>> bgColor) {
    animatorRef.set(new AtomicReference<Animator>(null));
    isCompleteRef.set(new AtomicReference<Boolean>(false));
    animatedValue.set(new DynamicValue<>(0f));

    Animations.bind(animatedValue)
        .with(
            new Interpolator() {
              private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

              @Override
              public float getInterpolation(float input) {
                input = (float) Math.sin(input * .5 * 2 * Math.PI);
                return (int) argbEvaluator.evaluate(input, Color.RED, Color.BLUE);
              }
            })
        .toInteger(bgColor);
  }

  @OnCreateLayout
  static Component onCreateLayout(final ComponentContext c, @State DynamicValue<Integer> bgColor) {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 20)
        .child(Row.create(c).heightDip(SIZE_DP).widthDip(SIZE_DP).backgroundColor(bgColor))
        .clickHandler(ColorTransition.onClickEvent(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(
      ComponentContext c,
      @State AtomicReference<Animator> animatorRef,
      @State AtomicReference<Boolean> isCompleteRef,
      @State DynamicValue<Float> animatedValue) {
    final boolean isComplete = Boolean.TRUE.equals(isCompleteRef.get());
    isCompleteRef.set(!isComplete);

    // Account for the progress of the previous animation in the duration
    final float animationProgress = animatedValue.get();
    final long animationDuration =
        (long) (ANIMATION_DURATION_MS * (isComplete ? animationProgress : 1 - animationProgress));

    Animations.animate(animatedValue)
        .to(isComplete ? 0 : 1)
        .duration(animationDuration)
        .interpolator(new LinearInterpolator())
        .startAndCancelPrevious(animatorRef);
  }
}
