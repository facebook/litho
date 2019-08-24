/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.animation;

import com.facebook.litho.TransitionId;

/**
 * Represents the animation specification for a single property: - What transition key it's for -
 * What property for that transition key that should animate - The value that property should
 * animate to
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

  public TransitionId getTransitionId() {
    return mPropertyHandle.getTransitionId();
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

    return Float.compare(that.mTargetValue, mTargetValue) == 0
        && mPropertyHandle.equals(that.mPropertyHandle);
  }

  @Override
  public int hashCode() {
    int result = mPropertyHandle.hashCode();
    result = 31 * result + (mTargetValue != +0.0f ? Float.floatToIntBits(mTargetValue) : 0);
    return result;
  }

  @Override
  public String toString() {
    return "PropertyAnimation{ PropertyHandle="
        + mPropertyHandle
        + ", TargetValue="
        + mTargetValue
        + "}";
  }
}
