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
 * A dimension-pair property (x, y) on a component that is appearing.
 */
public class AppearingPositionComponentProperty extends PositionComponentProperty {

  public AppearingPositionComponentProperty(
      AnimatedComponent animatedComponent,
      ComponentProperty xProperty,
      ComponentProperty yProperty) {
    super(animatedComponent, xProperty, yProperty);
  }

  public static class TransitionBuilder implements Animated.AnimationBuilder {

    private final Animated.AbstractPointBuilder mBuilderDelegate;
    private RuntimeValue mFromX;
    private RuntimeValue mFromY;

    public TransitionBuilder(Animated.AbstractPointBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on an appearing component from the given values.
     */
    public TransitionBuilder from(RuntimeValue fromX, RuntimeValue fromY) {
      mFromX = fromX;
      mFromY = fromY;
      return this;
    }

    @Override
    public AnimationBinding build() {
      return mBuilderDelegate.buildForAppear(mFromX, mFromY);
    }
  }
}
