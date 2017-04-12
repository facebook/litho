// Copyright 2004-present Facebook. All Rights Reserved.

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
    final ConstantNode<Float> initial =
        new ConstantNode(resolver.getCurrentState(mComponentProperty));
    final ConstantNode<Float> end = new ConstantNode(resolver.getEndState(mComponentProperty));

    addBinding(initial, springNode, SpringNode.INITIAL_INPUT);
    addBinding(end, springNode, SpringNode.END_INPUT);
    addBinding(springNode, resolver.getAnimatedPropertyNode(mComponentProperty));
  }
}
