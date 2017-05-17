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
 * A dimension property (x, y, width, height) on a component that is disappearing.
 */
public class DisappearingDimensionComponentProperty extends DimensionComponentProperty {

  public DisappearingDimensionComponentProperty(
      DisappearingComponent animatedComponent,
      AnimatedProperty property) {
    super(animatedComponent, property);
  }

  public static class TransitionBuilder implements Animated.AnimationBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;
    private DimensionValue mToValue;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on a disappearing component to the given absolute value.
     */
    public TransitionBuilder to(float value) {
      mToValue = DimensionValue.absolute(value);
      return this;
    }

    /**
     * Transition this property on a disappearing component to the given percentage of the mount
     * item's width.
     */
    public TransitionBuilder toOffsetByWidth(float percentage) {
      mToValue = DimensionValue.widthPercentageOffset(percentage);
      return this;
    }

    /**
     * Transition this property on a disappearing component starting to an offset equal to the mount
     * item's width.
     */
    public TransitionBuilder toOffsetByWidthLeft() {
      return toOffsetByWidth(-100);
    }

    /**
     * Transition this property on a disappearing component starting to an offset equal to the mount
     * item's width.
     */
    public TransitionBuilder toOffsetByWidthRight() {
      return toOffsetByWidth(100);
    }

    /**
     * Transition this property on a disappearing component to the given percentage of the mount
     * item's height.
     */
    public TransitionBuilder toOffsetByHeight(float percentage) {
      mToValue = DimensionValue.heightPercentageOffset(percentage);
      return this;
    }

    /**
     * Transition this property on a disappearing component starting to an offset equal to the mount
     * item's height.
     */
    public TransitionBuilder toOffsetByHeightAbove() {
      return toOffsetByHeight(-100);
    }

    /**
     * Transition this property on a disappearing component starting to an offset equal to the mount
     * item's height.
     */
    public TransitionBuilder toOffsetDownByHeight() {
      return toOffsetByHeight(100);
    }

    @Override
    public AnimationBinding build() {
      return mBuilderDelegate.buildForDisappear(mToValue);
    }
  }
}
