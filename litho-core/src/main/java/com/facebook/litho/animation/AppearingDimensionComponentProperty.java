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
 * A dimension property (x, y, width, height) on a component that is appearing.
 */
public class AppearingDimensionComponentProperty extends DimensionComponentProperty {

  public AppearingDimensionComponentProperty(
      AnimatedComponent AnimatedComponent,
      AnimatedProperty property) {
    super(AnimatedComponent, property);
  }

  public static class TransitionBuilder implements Animated.AnimationBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;
    private LazyDimensionValue mFromValue;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on an appearing component from the given absolute value.
     */
    public TransitionBuilder from(float value) {
      mFromValue = LazyDimensionValue.absolute(value);
      return this;
    }

    /**
     * Transition this property on an appearing component from the given percentage of the mount
     * item's width.
     */
    public TransitionBuilder fromOffsetByWidth(float percentage) {
      mFromValue = LazyDimensionValue.widthPercentageOffset(percentage);
      return this;
    }

    /**
     * Transition this property on an appearing component starting from an offset equal to the mount
     * item's width.
     */
    public TransitionBuilder fromOffsetByWidthLeft() {
      return fromOffsetByWidth(-100);
    }

    /**
     * Transition this property on an appearing component starting from an offset equal to the mount
     * item's width.
     */
    public TransitionBuilder fromOffsetByWidthRight() {
      return fromOffsetByWidth(100);
    }

    /**
     * Transition this property on an appearing component from the given percentage of the mount
     * item's height.
     */
    public TransitionBuilder fromOffsetByHeight(float percentage) {
      mFromValue = LazyDimensionValue.heightPercentageOffset(percentage);
      return this;
    }

    /**
     * Transition this property on an appearing component starting from an offset equal to the mount
     * item's height.
     */
    public TransitionBuilder fromOffsetByHeightAbove() {
      return fromOffsetByHeight(-100);
    }

    /**
     * Transition this property on an appearing component starting from an offset equal to the mount
     * item's height.
     */
    public TransitionBuilder fromOffsetDownByHeight() {
      return fromOffsetByHeight(100);
    }

    @Override
    public AnimationBinding build() {
      return mBuilderDelegate.buildForAppear(mFromValue);
    }
  }
}
