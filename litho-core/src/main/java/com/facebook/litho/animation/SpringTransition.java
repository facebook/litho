/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.ConstantNode;
import com.facebook.litho.dataflow.SpringNode;
import com.facebook.litho.dataflow.springs.SpringConfig;
import java.util.ArrayList;
import javax.annotation.Nullable;

/** Animation for the transition of a single {@link PropertyAnimation} on a spring. */
public class SpringTransition extends TransitionAnimationBinding {

  private final PropertyAnimation mPropertyAnimation;
  private final @Nullable SpringConfig mSpringConfig;

  public SpringTransition(PropertyAnimation propertyAnimation, SpringConfig springConfig) {
    mPropertyAnimation = propertyAnimation;
    mSpringConfig = springConfig;
  }

  public SpringTransition(PropertyAnimation propertyAnimation) {
    this(propertyAnimation, null);
  }

  @Override
  public void collectTransitioningProperties(ArrayList<PropertyAnimation> outList) {
    outList.add(mPropertyAnimation);
  }

  @Override
  protected void setupBinding(Resolver resolver) {
    final SpringNode springNode = new SpringNode(mSpringConfig);
    final ConstantNode initial =
        new ConstantNode(resolver.getCurrentState(mPropertyAnimation.getPropertyHandle()));
    final ConstantNode end = new ConstantNode(mPropertyAnimation.getTargetValue());

    addBinding(initial, springNode, SpringNode.INITIAL_INPUT);
    addBinding(end, springNode, SpringNode.END_INPUT);
    addBinding(
        springNode, resolver.getAnimatedPropertyNode(mPropertyAnimation.getPropertyHandle()));
  }
}
