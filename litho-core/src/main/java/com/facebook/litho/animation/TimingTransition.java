/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.animation;

import android.support.annotation.Nullable;
import android.view.animation.Interpolator;
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
    final ConstantNode initial = new ConstantNode(resolver.getCurrentState(mPropertyAnimation.getPropertyHandle()));
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
