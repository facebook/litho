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
import com.facebook.litho.internal.ArraySet;

/**
 * Animation for the transition of a single {@link ComponentProperty} on a spring.
 */
public class SpringTransition extends TransitionAnimationBinding {

  private final ComponentProperty mComponentProperty;

  public SpringTransition(ComponentProperty property) {
    mComponentProperty = property;
  }

  @Override
  public void collectTransitioningProperties(ArraySet<ComponentProperty> outSet) {
    outSet.add(mComponentProperty);
  }

  @Override
  protected void setupBinding(Resolver resolver) {
    final SpringNode springNode = new SpringNode();
    final ConstantNode initial = new ConstantNode(resolver.getCurrentState(mComponentProperty));
    final ConstantNode end = new ConstantNode(resolver.getEndState(mComponentProperty));

    addBinding(initial, springNode, SpringNode.INITIAL_INPUT);
    addBinding(end, springNode, SpringNode.END_INPUT);
    addBinding(springNode, resolver.getAnimatedPropertyNode(mComponentProperty));
  }
}
