// Copyright 2004-present Facebook. All Rights Reserved.

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

  public static class TransitionBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;
    private LazyDimensionValue mToValue;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on a disappearing component to the given absolute value.
     */
    public TransitionBuilder to(float value) {
      mToValue = LazyDimensionValue.absolute(value);
      return this;
    }

    /**
     * Transition this property on a disappearing component to the given percentage of the mount
     * item's width.
     */
    public TransitionBuilder toOffsetByWidth(float percentage) {
      mToValue = LazyDimensionValue.widthPercentageOffset(percentage);
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
      mToValue = LazyDimensionValue.heightPercentageOffset(percentage);
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

    public AnimationBinding build() {
      return mBuilderDelegate.buildForDisappear(mToValue);
    }
  }
}
