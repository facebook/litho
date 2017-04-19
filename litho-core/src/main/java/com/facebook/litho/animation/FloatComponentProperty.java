/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.animation;

/**
 * A property (e.g. scale, opacity) on a component that's changing.
 */
public class FloatComponentProperty extends ComponentProperty {

  public FloatComponentProperty(
      AnimatedComponent animatedComponent,
      AnimatedProperty property) {
    super(animatedComponent, property);
  }

  public static class TransitionBuilder implements Animated.AnimationBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    @Override
    public AnimationBinding build() {
      return mBuilderDelegate.buildForChange();
    }
  }
}
