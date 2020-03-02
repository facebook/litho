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
import android.graphics.Path;
import android.graphics.PathMeasure;
import androidx.annotation.Dimension;
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
class PathExampleComponentSpec {

  private static final long ANIMATION_DURATION_MS = 3000;
  private static final @Dimension(unit = Dimension.DP) int SQUARE_SIDE_LENGTH = 50;

  private static Animator createPathAnimator(
      ComponentContext c,
      final DynamicValue<Float> translationX,
      final DynamicValue<Float> translationY) {
    final @Dimension int sideLength = c.getResourceResolver().dipsToPixels(SQUARE_SIDE_LENGTH);
    final Path squarePath = new Path();
    squarePath.moveTo(-sideLength, -sideLength);
    squarePath.lineTo(sideLength, -sideLength);
    squarePath.lineTo(sideLength, sideLength);
    squarePath.lineTo(-sideLength, sideLength);
    squarePath.lineTo(-sideLength, -sideLength);
    squarePath.close();

    final ValueAnimator pathAnimator = ValueAnimator.ofFloat(0f, 1f);
    pathAnimator.setDuration(ANIMATION_DURATION_MS);
    pathAnimator.setRepeatCount(ValueAnimator.INFINITE);
    pathAnimator.setInterpolator(null);
    pathAnimator.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {

          private final float[] point = new float[2];

          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
            float fraction = animation.getAnimatedFraction();
            PathMeasure pathMeasure = new PathMeasure(squarePath, true);
            pathMeasure.getPosTan(pathMeasure.getLength() * fraction, point, null);
            // Update the dynamic values with path coordinates
            translationX.set(point[0]);
            translationY.set(point[1]);
          }
        });
    return pathAnimator;
  }

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<Animator> animator,
      StateValue<DynamicValue<Float>> translationX,
      StateValue<DynamicValue<Float>> translationY) {
    // Set up the DynamicProps and an Animator to manipulate them. Making them State values for easy
    // sharing between the static functions.
    translationX.set(new DynamicValue<>(0f));
    translationY.set(new DynamicValue<>(0f));
    animator.set(createPathAnimator(c, translationX.get(), translationY.get()));
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State DynamicValue<Float> translationX,
      @State DynamicValue<Float> translationY) {
    return Column.create(c)
        .alignItems(YogaAlign.CENTER)
        .justifyContent(YogaJustify.CENTER)
        .paddingDip(YogaEdge.ALL, 50)
        .child(
            // Create the component we will animate. Apply the DynamicValues to it.
            Text.create(c)
                .text("\u26BE")
                .textSizeSp(50)
                .translationX(translationX)
                .translationY(translationY))
        .visibleHandler(ContinuousExampleComponent.onVisible(c))
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(ComponentContext c, @State Animator animator) {
    animator.start();
  }
}
