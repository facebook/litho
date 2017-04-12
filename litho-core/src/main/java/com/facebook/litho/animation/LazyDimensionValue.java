// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A {@link LazyValue} for dimension properties like x/y/width/height.
 */
public class LazyDimensionValue implements LazyValue {

  /**
   * @return a LazyValue that resolves to an absolute value.
   */
  public static LazyDimensionValue absolute(float value) {
    return new LazyDimensionValue(Type.ABSOLUTE, value);
  }

  /**
   * @return a LazyValue that resolves to an offset relative to the current value of some mount item
   * property.
   */
  public static LazyDimensionValue offset(float value) {
    return new LazyDimensionValue(Type.OFFSET, value);
  }

  /**
   * Like {@link #offset}, but the relative offset is based on a percentage of the mount item width.
   */
  public static LazyDimensionValue widthPercentageOffset(float value) {
    return new LazyDimensionValue(Type.OFFSET_WIDTH_PERCENTAGE, value);
  }

  /**
   * Like {@link #offset}, but the relative offset is based on a percentage of the mount item height
   */
  public static LazyDimensionValue heightPercentageOffset(float value) {
    return new LazyDimensionValue(Type.OFFSET_HEIGHT_PERCENTAGE, value);
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

  private LazyDimensionValue(Type type, float value) {
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
        throw new RuntimeException("Missing LazyValue type: " + mType);
    }
  }
}
