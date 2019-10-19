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

import android.view.animation.Interpolator;
import androidx.annotation.Nullable;
import com.facebook.litho.dataflow.ConstantNode;
import com.facebook.litho.dataflow.InterpolatorNode;
import com.facebook.litho.dataflow.MappingNode;
import com.facebook.litho.dataflow.TimingNode;
import java.util.ArrayList;

/**
 * Animation for the transition of a single {@link PropertyAnimation} over a fixed amount of time.
 */
public class TimingTransition extends TransitionAnimationBinding {

  private final int mDurationMs;
  private final PropertyAnimation mPropertyAnimation;
  private final @Nullable Interpolator mInterpolator;

  public TimingTransition(int durationMs, PropertyAnimation propertyAnimation) {
    this(durationMs, propertyAnimation, null);
  }

  public TimingTransition(
      int durationMs, PropertyAnimation propertyAnimation, Interpolator interpolator) {
    mDurationMs = durationMs;
    mPropertyAnimation = propertyAnimation;
    mInterpolator = interpolator;
  }

  @Override
  public void collectTransitioningProperties(ArrayList<PropertyAnimation> outList) {
    outList.add(mPropertyAnimation);
  }

  @Override
  protected void setupBinding(Resolver resolver) {
    final TimingNode timingNode = new TimingNode(mDurationMs);
    final ConstantNode initial =
        new ConstantNode(resolver.getCurrentState(mPropertyAnimation.getPropertyHandle()));
    final ConstantNode end = new ConstantNode(mPropertyAnimation.getTargetValue());
    final MappingNode mappingNode = new MappingNode();

    if (mInterpolator != null) {
      final InterpolatorNode interpolatorNode = new InterpolatorNode(mInterpolator);
      addBinding(timingNode, interpolatorNode);
      addBinding(interpolatorNode, mappingNode);
    } else {
      addBinding(timingNode, mappingNode);
    }
    addBinding(initial, mappingNode, MappingNode.INITIAL_INPUT);
    addBinding(end, mappingNode, MappingNode.END_INPUT);
    addBinding(
        mappingNode, resolver.getAnimatedPropertyNode(mPropertyAnimation.getPropertyHandle()));
  }
}
