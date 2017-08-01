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
 * Represents the animation specification for a single property:
 *  - What transition key it's for
 *  - What property for that transition key that should animate
 *  - The value that property should animate to
 */
public class PropertyAnimation {

  private final PropertyHandle mPropertyHandle;
  private final float mTargetValue;

  public PropertyAnimation(PropertyHandle propertyHandle, float targetValue) {
    mPropertyHandle = propertyHandle;
    mTargetValue = targetValue;
  }

  public PropertyHandle getPropertyHandle() {
    return mPropertyHandle;
  }

  public String getTransitionKey() {
    return mPropertyHandle.getTransitionKey();
  }

  public AnimatedProperty getProperty() {
    return mPropertyHandle.getProperty();
  }

  public float getTargetValue() {
    return mTargetValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PropertyAnimation that = (PropertyAnimation) o;

    return Float.compare(that.mTargetValue, mTargetValue) == 0 &&
        mPropertyHandle.equals(that.mPropertyHandle);
  }

  @Override
  public int hashCode() {
    int result = mPropertyHandle.hashCode();
    result = 31 * result + (mTargetValue != +0.0f ? Float.floatToIntBits(mTargetValue) : 0);
    return result;
  }

  @Override
  public String toString() {
    return "PropertyAnimation{ PropertyHandle=" + mPropertyHandle + ", TargetValue=" +
        mTargetValue + "}";
  }
}
