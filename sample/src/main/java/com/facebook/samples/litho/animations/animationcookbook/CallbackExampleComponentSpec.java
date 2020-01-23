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

package com.facebook.samples.litho.animations.animationcookbook;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;
import java.util.concurrent.atomic.AtomicReference;

@LayoutSpec
class CallbackExampleComponentSpec {

  private static final long ANIMATION_DURATION_MS = 1000;
  private static final float SCALE_TO = 1.7f;

  private static Animator createScaleAnimator(final DynamicValue<Float> scale) {
    final ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1f, SCALE_TO, 1f);
    scaleAnimator.setDuration(ANIMATION_DURATION_MS);
    scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    scaleAnimator.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
            scale.set((Float) animation.getAnimatedValue());
          }
        });
    return scaleAnimator;
  }

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<AtomicReference<Animator>> animatorRef,
      StateValue<DynamicValue<Float>> scale) {
    animatorRef.set(new AtomicReference<Animator>(null));
    scale.set(new DynamicValue<>(1f));
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State DynamicValue<Float> scale) {
    return Column.create(c)
        .alignItems(YogaAlign.CENTER)
        .justifyContent(YogaJustify.CENTER)
        .paddingDip(YogaEdge.ALL, 50)
        .clickHandler(CallbackExampleComponent.onClick(c))
        .child(Text.create(c).text("\uD83D\uDE18").textSizeSp(50).scaleX(scale).scaleY(scale))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      final ComponentContext c,
      @State AtomicReference<Animator> animatorRef,
      @State DynamicValue<Float> scale) {
    Animator oldAnimator = animatorRef.get();
    if (oldAnimator != null) {
      oldAnimator.cancel();
    }

    final Animator newAnimator = createScaleAnimator(scale);
    animatorRef.set(newAnimator);

    newAnimator.addListener(
        new Animator.AnimatorListener() {
          @Override
          public void onAnimationStart(Animator animation) {}

          @Override
          public void onAnimationEnd(Animator animation) {
            Toast.makeText(c.getAndroidContext(), "Animation finished", Toast.LENGTH_SHORT).show();
          }

          @Override
          public void onAnimationCancel(Animator animation) {}

          @Override
          public void onAnimationRepeat(Animator animation) {}
        });

    newAnimator.start();
  }
}
