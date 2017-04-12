// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.ConstantNode;
import com.facebook.litho.dataflow.TimingNode;
import com.facebook.litho.internal.ArraySet;

/**
 * Animation for the transition of a single {@link ComponentProperty} over a fixed amount of time.
 */
public class TimingTransition extends TransitionAnimationBinding {

  private final int mDurationMs;
  private final ComponentProperty mComponentProperty;

  public TimingTransition(int durationMs, ComponentProperty property) {
    mDurationMs = durationMs;
    mComponentProperty = property;
  }

  @Override
  public void collectTransitioningProperties(ArraySet<ComponentProperty> outSet) {
    outSet.add(mComponentProperty);
  }

  @Override
  protected void setupBinding(Resolver resolver) {
    final TimingNode timingNode = new TimingNode(mDurationMs);
    final ConstantNode<Float> initial =
        new ConstantNode(resolver.getCurrentState(mComponentProperty));
    final ConstantNode<Float> end = new ConstantNode(resolver.getEndState(mComponentProperty));

    addBinding(initial, timingNode, TimingNode.INITIAL_INPUT);
    addBinding(end, timingNode, TimingNode.END_INPUT);
    addBinding(timingNode, resolver.getAnimatedPropertyNode(mComponentProperty));
  }
}
