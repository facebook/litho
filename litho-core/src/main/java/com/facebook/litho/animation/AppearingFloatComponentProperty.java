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
 * A property (e.g. scale, opacity) on a component that's appearing.
 */
public class AppearingFloatComponentProperty extends FloatComponentProperty {

  public AppearingFloatComponentProperty(
      AppearingComponent animatedComponent,
      AnimatedProperty property) {
    super(animatedComponent, property);
  }

  public static class TransitionBuilder implements Animated.AnimationBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;
    private FloatValue mFromValue;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on an appearing component from the given absolute value.
     */
    public TransitionBuilder from(float value) {
      mFromValue = new FloatValue(value);
      return this;
    }

    @Override
    public AnimationBinding build() {
      return mBuilderDelegate.buildForAppear(mFromValue);
    }
  }
}
