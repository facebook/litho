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
 * A dimension-pair property (x, y) on a component that is changing.
 */
public class PositionComponentProperty extends CompositeComponentProperty {

  private final ComponentProperty mXProperty;
  private final ComponentProperty mYProperty;

  public PositionComponentProperty(
      AnimatedComponent animatedComponent,
      ComponentProperty xProperty,
      ComponentProperty yProperty) {
    super(animatedComponent);
    mXProperty = xProperty;
    mYProperty = yProperty;
  }

  /**
   * @return the {@link ComponentProperty} corresponding to the x property of this point.
   */
  public ComponentProperty getXProperty() {
    return mXProperty;
  }

  /**
   * @return the {@link ComponentProperty} corresponding to the y property of this point.
   */
  public ComponentProperty getYProperty() {
    return mYProperty;
  }

  public static class TransitionBuilder implements Animated.AnimationBuilder {

    private final Animated.AbstractPointBuilder mBuilderDelegate;

    public TransitionBuilder(Animated.AbstractPointBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    @Override
    public AnimationBinding build() {
      return mBuilderDelegate.buildForChange();
    }
  }
}
