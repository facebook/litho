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
 * A property (e.g. scale, opacity) on a component that's disappearing.
 */
public class DisappearingFloatComponentProperty extends FloatComponentProperty {

  public DisappearingFloatComponentProperty(
      DisappearingComponent animatedComponent,
      AnimatedProperty property) {
    super(animatedComponent, property);
  }

  public static class TransitionBuilder implements Animated.AnimationBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;
    private LazyFloatValue mToValue;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on a disappearing component to the given absolute value.
     */
    public TransitionBuilder to(float value) {
      mToValue = new LazyFloatValue(value);
      return this;
    }

    @Override
    public AnimationBinding build() {
      return mBuilderDelegate.buildForDisappear(mToValue);
    }
  }
}
