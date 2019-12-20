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

package com.facebook.samples.litho.animations.commondynamicprops;

import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class CommonDynamicPropsAnimationActivity extends NavigatableDemoActivity {

  private static final long ONE_SECOND = TimeUnit.SECONDS.toMillis(1);
  private static final float MAX_SCALE = 1.2f;
  private static final float MIN_SCALE = 0.8f;
  private static final @Dimension float MAX_TRANSLATION = 50f;
  private static final @ColorInt int START_COLOR = Color.BLUE;
  private static final @ColorInt int END_COLOR = Color.MAGENTA;

  private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
  private final FloatEvaluator floatEvaluator = new FloatEvaluator();
  private final DynamicValue<Float> alpha = new DynamicValue<>(0f);
  private final DynamicValue<Float> scale = new DynamicValue<>(0f);
  private final DynamicValue<Float> translation = new DynamicValue<>(0f);
  private final DynamicValue<Integer> backgroundColor = new DynamicValue<>(START_COLOR);
  private final DynamicValue<Float> rotation = new DynamicValue<>(0f);
  private final DynamicValue<Float> elevation = new DynamicValue<>(0f);

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
    animator.setDuration(ONE_SECOND);
    animator.setRepeatMode(ValueAnimator.REVERSE);
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
            float d = animation.getAnimatedFraction();
            alpha.set(d);
            scale.set(floatEvaluator.evaluate(d, MIN_SCALE, MAX_SCALE));
            translation.set(floatEvaluator.evaluate(d, 0, MAX_TRANSLATION));
            backgroundColor.set((int) argbEvaluator.evaluate(d, START_COLOR, END_COLOR));
            rotation.set(floatEvaluator.evaluate(d, 0, 360));
            elevation.set(floatEvaluator.evaluate(d, 0, 50));
          }
        });
    animator.start();

    final ComponentContext componentContext = new ComponentContext(this);
    setContentView(
        LithoView.create(
            this,
            CommonDynamicPropAnimationsComponent.create(componentContext)
                .dynamicAlpha(alpha)
                .dynamicScale(scale)
                .dynamicTranslation(translation)
                .dynamicBgColor(backgroundColor)
                .dynamicRotation(rotation)
                .dynamicElevation(elevation)
                .build()));
  }
}
