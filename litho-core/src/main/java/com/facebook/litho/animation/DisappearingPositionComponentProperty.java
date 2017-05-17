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
 * A dimension-pair property (x, y) on a component that is disappearing.
 */
public class DisappearingPositionComponentProperty extends PositionComponentProperty {

  public DisappearingPositionComponentProperty(
      DisappearingComponent animatedComponent,
      ComponentProperty xProperty,
      ComponentProperty yProperty) {
    super(animatedComponent, xProperty, yProperty);
  }

  public static class TransitionBuilder implements Animated.AnimationBuilder {

    private final Animated.AbstractPointBuilder mBuilderDelegate;
    private RuntimeValue mToX;
    private RuntimeValue mToY;

    public TransitionBuilder(Animated.AbstractPointBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on a disappearing component to the given values.
     */
    public TransitionBuilder from(RuntimeValue toX, RuntimeValue toY) {
      mToX = toX;
      mToY = toY;
      return this;
    }

    @Override
    public AnimationBinding build() {
      return mBuilderDelegate.buildForDisappear(mToX, mToY);
    }
  }
}
