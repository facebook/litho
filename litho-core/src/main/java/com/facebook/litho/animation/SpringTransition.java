/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.animation;

import com.facebook.litho.dataflow.ConstantNode;
import com.facebook.litho.dataflow.SpringNode;
import java.util.ArrayList;

/**
 * Animation for the transition of a single {@link PropertyAnimation} on a spring.
 */
public class SpringTransition extends TransitionAnimationBinding {

  private final PropertyAnimation mPropertyAnimation;

  public SpringTransition(PropertyAnimation propertyAnimation) {
    mPropertyAnimation = propertyAnimation;
  }

  @Override
  public void collectTransitioningProperties(ArrayList<PropertyAnimation> outList) {
    outList.add(mPropertyAnimation);
  }

  @Override
  protected void setupBinding(Resolver resolver) {
    final SpringNode springNode = new SpringNode();
    final ConstantNode initial = new ConstantNode(resolver.getCurrentState(mPropertyAnimation.getPropertyHandle()));
    final ConstantNode end = new ConstantNode(mPropertyAnimation.getTargetValue());

    addBinding(initial, springNode, SpringNode.INITIAL_INPUT);
    addBinding(end, springNode, SpringNode.END_INPUT);
    addBinding(springNode, resolver.getAnimatedPropertyNode(mPropertyAnimation.getPropertyHandle()));
  }
}
