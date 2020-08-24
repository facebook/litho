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

package com.facebook.litho.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionValues;
import com.facebook.litho.BoundsHelper;
import com.facebook.litho.ComponentHost;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link androidx.transition.Transition} to change the bounds of a {@link ComponentHost}. This is
 * required as the {@link ComponentHost}'s linked drawables must have their size adjusted on every
 * frame.
 */
public class ComponentHostChangeBoundsTransition extends ChangeBounds {
  @Override
  public @Nullable Animator createAnimator(
      final ViewGroup sceneRoot,
      @Nullable TransitionValues startValues,
      @Nullable final TransitionValues endValues) {

    Animator animator = super.createAnimator(sceneRoot, startValues, endValues);

    // super.createAnimator(..) will return null, an ObjectAnimator, or an AnimatorSet (of
    // ObjectAnimators). Only ObjectAnimator allows adding update listeners so depending on the type
    // we can add it directly or to one of the immediate children.
    // Note: it is not possible to use a View.OnLayoutChangeListener here.

    if (animator == null || endValues == null || !(endValues.view instanceof ComponentHost)) {
      return animator;
    }

    Animator forListener = animator;

    if (animator instanceof AnimatorSet) {
      ArrayList<Animator> animators = ((AnimatorSet) animator).getChildAnimations();
      // Get the last animator so the callback occurs after all frame operations.
      forListener = animators.get(animators.size() - 1);
    }

    if (forListener instanceof ValueAnimator) {
      ((ValueAnimator) forListener)
          .addUpdateListener(
              new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                  final List<Drawable> animatingDrawables =
                      ((ComponentHost) endValues.view).getLinkedDrawablesForAnimation();
                  if (animatingDrawables == null) {
                    return;
                  }
                  for (int index = 0; index < animatingDrawables.size(); ++index) {
                    BoundsHelper.applySizeToDrawableForAnimation(
                        animatingDrawables.get(index),
                        endValues.view.getRight() - endValues.view.getLeft(),
                        endValues.view.getBottom() - endValues.view.getTop());
                  }
                }
              });
    }

    return animator;
  }
}
