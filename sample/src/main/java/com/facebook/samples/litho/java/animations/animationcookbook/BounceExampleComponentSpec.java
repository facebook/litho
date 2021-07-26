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
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.animation.BounceInterpolator;
import androidx.annotation.Dimension;
import androidx.annotation.Px;
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
class BounceExampleComponentSpec {

  private static final long UP_DURATION_MS = 200;
  private static final long BOUNCE_DURATION_MS = 800;
  private static final @Dimension(unit = Dimension.DP) float BOUNCE_HEIGHT_DP = -50;

  private static Animator createBounceAnimator(
      ComponentContext c, final DynamicValue<Float> translationY) {
    final @Px float bounceHeightPx = c.getResourceResolver().dipsToPixels(BOUNCE_HEIGHT_DP);

    // 1. Animate the component from its current position to `bounceHeightPx`
    final @Px float currentTranslationX = translationY.get();
    final @Px float deltaX = Math.abs(bounceHeightPx - currentTranslationX);
    final float time = deltaX / Math.abs(bounceHeightPx) * UP_DURATION_MS;
    final ValueAnimator upAnimator = ValueAnimator.ofFloat(currentTranslationX, bounceHeightPx);
    upAnimator.setDuration((long) time);
    upAnimator.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
            translationY.set((Float) animation.getAnimatedValue());
          }
        });

    // 2. Animate the component back to 0 using a bounce interpolator
    final ValueAnimator bounceAnimator = ValueAnimator.ofFloat(bounceHeightPx, 0);
    bounceAnimator.setDuration(BOUNCE_DURATION_MS);
    bounceAnimator.setInterpolator(new BounceInterpolator());
    bounceAnimator.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
            translationY.set((Float) animation.getAnimatedValue());
          }
        });

    final AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.playSequentially(upAnimator, bounceAnimator);
    return animatorSet;
  }

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<AtomicReference<Animator>> animatorRef,
      StateValue<DynamicValue<Float>> translationY) {
    animatorRef.set(new AtomicReference<Animator>(null));
    translationY.set(new DynamicValue<>(0f));
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State DynamicValue<Float> translationY) {
    return Column.create(c)
        .alignItems(YogaAlign.CENTER)
        .justifyContent(YogaJustify.CENTER)
        .paddingDip(YogaEdge.ALL, 50)
        .clickHandler(BounceExampleComponent.onClick(c))
        .child(Text.create(c).text("\u26BD").textSizeSp(50).translationY(translationY))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @State AtomicReference<Animator> animatorRef,
      @State DynamicValue<Float> translationY) {
    Animator oldAnimator = animatorRef.get();
    if (oldAnimator != null) {
      oldAnimator.cancel();
    }

    final Animator newAnimator = createBounceAnimator(c, translationY);
    animatorRef.set(newAnimator);
    newAnimator.start();
  }
}
