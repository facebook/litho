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

package com.facebook.samples.litho.java.animations.animationcookbook;

import android.animation.Animator;
import android.animation.FloatEvaluator;
import android.animation.TimeAnimator;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.StateValue;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;

@LayoutSpec
class ContinuousExampleComponentSpec {

  private static final long ANIMATION_DURATION_MS = 3000;

  private static Animator createRotationAnimator(final DynamicValue<Float> rotation) {
    final TimeAnimator animator = new TimeAnimator();
    final FloatEvaluator floatEvaluator = new FloatEvaluator();
    animator.setTimeListener(
        new TimeAnimator.TimeListener() {
          @Override
          public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
            float fraction =
                Float.valueOf(totalTime % ANIMATION_DURATION_MS) / ANIMATION_DURATION_MS;
            rotation.set(fraction * 360);
          }
        });
    return animator;
  }

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c, StateValue<Animator> animator, StateValue<DynamicValue<Float>> rotation) {
    rotation.set(new DynamicValue<>(0f));
    animator.set(createRotationAnimator(rotation.get()));
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State DynamicValue<Float> rotation) {
    return Column.create(c)
        .alignItems(YogaAlign.CENTER)
        .justifyContent(YogaJustify.CENTER)
        .paddingDip(YogaEdge.ALL, 50)
        .child(Text.create(c).text("\u2B50").textSizeSp(50).rotation(rotation))
        .visibleHandler(ContinuousExampleComponent.onVisible(c))
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(ComponentContext c, @State Animator animator) {
    animator.start();
  }
}
