/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.animation;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * A {@link RuntimeValue} for dimension properties like x/y/width/height.
 */
public class DimensionValue implements RuntimeValue {

  /**
   * @return a RuntimeValue that resolves to an absolute value.
   */
  public static DimensionValue absolute(float value) {
    return new DimensionValue(Type.ABSOLUTE, value);
  }

  /**
   * @return a RuntimeValue that resolves to an offset relative to the current value of some mount item
   * property.
   */
  public static DimensionValue offsetPx(float value) {
    return new DimensionValue(Type.OFFSET, value);
  }

  /**
   * @return a RuntimeValue that resolves to an offset relative to the current value of some mount item
   * property.
   */
  public static DimensionValue offsetDip(Context context, int valueDp) {
    final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    final float valuePx = displayMetrics.density * valueDp;
    return offsetPx(valuePx);
  }

  /**
   * Like {@link #offsetPx}, but the relative offset is based on a percentage of the mount item width.
   */
  public static DimensionValue widthPercentageOffset(float value) {
    return new DimensionValue(Type.OFFSET_WIDTH_PERCENTAGE, value);
  }

  /**
   * Like {@link #offsetPx}, but the relative offset is based on a percentage of the mount item height
   */
  public static DimensionValue heightPercentageOffset(float value) {
    return new DimensionValue(Type.OFFSET_HEIGHT_PERCENTAGE, value);
  }

  private enum Type {
    ABSOLUTE,
    OFFSET,
    OFFSET_WIDTH_PERCENTAGE,
    OFFSET_HEIGHT_PERCENTAGE
    ;
  }

  private final Type mType;
  private final float mValue;

  private DimensionValue(Type type, float value) {
    mType = type;
    mValue = value;
  }

  public float resolve(Resolver resolver, ComponentProperty componentProperty) {
    final float currentValue = resolver.getCurrentState(componentProperty);
    switch (mType) {
      case ABSOLUTE:
        return mValue;
      case OFFSET:
        return mValue + currentValue;
      case OFFSET_WIDTH_PERCENTAGE:
        final float width =
            resolver.getCurrentState(componentProperty.getAnimatedComponent().width());
        return mValue / 100 * width + currentValue;
      case OFFSET_HEIGHT_PERCENTAGE:
        final float height =
            resolver.getCurrentState(componentProperty.getAnimatedComponent().height());
        return mValue / 100 * height + currentValue;
      default:
        throw new RuntimeException("Missing RuntimeValue type: " + mType);
    }
  }
}
