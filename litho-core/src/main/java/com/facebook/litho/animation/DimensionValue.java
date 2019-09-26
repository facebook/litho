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

import android.content.Context;
import android.util.DisplayMetrics;

/** A {@link RuntimeValue} for dimension properties like x/y/width/height. */
public class DimensionValue implements RuntimeValue {

  /** @return a RuntimeValue that resolves to an absolute value. */
  public static DimensionValue absolute(float value) {
    return new DimensionValue(Type.ABSOLUTE, value);
  }

  /**
   * @return a RuntimeValue that resolves to an offset relative to the current value of some mount
   *     content property.
   */
  public static DimensionValue offsetPx(float value) {
    return new DimensionValue(Type.OFFSET, value);
  }

  /**
   * @return a RuntimeValue that resolves to an offset relative to the current value of some mount
   *     content property.
   */
  public static DimensionValue offsetDip(Context context, int valueDp) {
    final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    final float valuePx = displayMetrics.density * valueDp;
    return offsetPx(valuePx);
  }

  /**
   * Like {@link #offsetPx}, but the relative offset is based on a percentage of the mount content
   * width.
   */
  public static DimensionValue widthPercentageOffset(float value) {
    return new DimensionValue(Type.OFFSET_WIDTH_PERCENTAGE, value);
  }

  /**
   * Like {@link #offsetPx}, but the relative offset is based on a percentage of the mount content
   * height
   */
  public static DimensionValue heightPercentageOffset(float value) {
    return new DimensionValue(Type.OFFSET_HEIGHT_PERCENTAGE, value);
  }

  private enum Type {
    ABSOLUTE,
    OFFSET,
    OFFSET_WIDTH_PERCENTAGE,
    OFFSET_HEIGHT_PERCENTAGE;
  }

  private final Type mType;
  private final float mValue;

  private DimensionValue(Type type, float value) {
    mType = type;
    mValue = value;
  }

  @Override
  public float resolve(Resolver resolver, PropertyHandle propertyHandle) {
    final float currentValue = resolver.getCurrentState(propertyHandle);
    switch (mType) {
      case ABSOLUTE:
        return mValue;
      case OFFSET:
        return mValue + currentValue;
      case OFFSET_WIDTH_PERCENTAGE:
        final float width =
            resolver.getCurrentState(
                new PropertyHandle(propertyHandle.getTransitionId(), AnimatedProperties.WIDTH));
        return mValue / 100 * width + currentValue;
      case OFFSET_HEIGHT_PERCENTAGE:
        final float height =
            resolver.getCurrentState(
                new PropertyHandle(propertyHandle.getTransitionId(), AnimatedProperties.HEIGHT));
        return mValue / 100 * height + currentValue;
      default:
        throw new RuntimeException("Missing RuntimeValue type: " + mType);
    }
  }
}
