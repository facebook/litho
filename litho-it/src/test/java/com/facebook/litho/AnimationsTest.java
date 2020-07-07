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

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.animation.Animator;
import android.view.animation.Interpolator;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class AnimationsTest {

  @Test
  public void animations_bind_changeInSourcePropagatesToBinding() {
    final DynamicValue<Float> source = new DynamicValue<>(5f);
    final StateValue<DynamicValue<Float>> target = new StateValue<>();
    Animations.bind(source).to(target);

    assertThat(target.get().get()).isEqualTo(5);

    source.set(2f);
    assertThat(target.get().get()).isEqualTo(2);
  }

  @Test
  public void animations_bindWithFromRange_rangeIsApplied() {
    final DynamicValue<Float> source = new DynamicValue<>(0f);
    final StateValue<DynamicValue<Float>> target = new StateValue<>();
    Animations.bind(source).inputRange(45, 55).to(target);

    assertThat(target.get().get()).isEqualTo(0);

    source.set(45f);
    assertThat(target.get().get()).isEqualTo(0);

    source.set(55f);
    assertThat(target.get().get()).isEqualTo(1);

    source.set(100f);
    assertThat(target.get().get()).isEqualTo(1);
  }

  @Test
  public void animations_bindWithToRange_rangeIsApplied() {
    final DynamicValue<Float> source = new DynamicValue<>(0f);
    final StateValue<DynamicValue<Float>> target = new StateValue<>();
    Animations.bind(source).outputRange(45, 55).to(target);

    assertThat(target.get().get()).isEqualTo(45);

    source.set(1f);
    assertThat(target.get().get()).isEqualTo(55);

    source.set(.5f);
    assertThat(target.get().get()).isEqualTo(50);
  }

  @Test
  public void animations_bindWithInterpolator_interpolatorIsApplied() {
    Interpolator mockInterpolator = mock(Interpolator.class);
    when(mockInterpolator.getInterpolation(anyFloat())).thenReturn(1f);

    final DynamicValue<Float> source = new DynamicValue<>(0f);
    final StateValue<DynamicValue<Float>> target = new StateValue<>();
    Animations.bind(source).with(mockInterpolator).to(target);

    assertThat(target.get().get()).isEqualTo(1f);
    verify(mockInterpolator).getInterpolation(eq(0f));
  }

  @Test
  public void animations_startAnimator_animatorStarted() {
    final DynamicValue<Float> in = new DynamicValue<>(0f);

    final StateValue<DynamicValue<Float>> outState = new StateValue<>();

    Animations.bind(in).outputRange(0, 5).to(outState);

    Animator animator = Animations.animate(in).to(1).duration(2000).start();

    assertThat(animator.getDuration()).isEqualTo(2000);
    animator.end();
    assertThat(outState.get().get()).isEqualTo(5);
  }

  @Test
  public void animations_startAnimatorAncCancelPrevious_previousIsCancelled() {
    final DynamicValue<Float> in = new DynamicValue<>(0f);

    final StateValue<DynamicValue<Float>> outState = new StateValue<>();

    Animations.bind(in).outputRange(0, 5).to(outState);

    Animator oldAnimator = mock(Animator.class);
    AtomicReference<Animator> animatorRef = new AtomicReference<>(oldAnimator);

    Animations.animate(in).to(1).duration(2000).startAndCancelPrevious(animatorRef);

    verify(oldAnimator).cancel();

    Animator newAnimator = animatorRef.get();
    assertThat(oldAnimator).isNotSameAs(newAnimator);
    assertThat(newAnimator.getDuration()).isEqualTo(2000);
    newAnimator.end();
    assertThat(outState.get().get()).isEqualTo(5);
  }
}
