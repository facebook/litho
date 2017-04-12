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

  private enum Type {
    ABSOLUTE,
    OFFSET,
    ;
  }

  private final Type mType;
  private final float mValue;

  private LazyDimensionValue(Type type, float value) {
    mType = type;
    mValue = value;
  }

  /**
   * @return a resolved value for this LazyValue based on the current state of the given mount item.
   */
  public float resolve(Resolver resolver, ComponentProperty componentProperty) {
    float currentValue = resolver.getCurrentState(componentProperty);
    switch (mType) {
      case ABSOLUTE:
        return mValue;
      case OFFSET:
        return mValue + currentValue;
      default:
        throw new RuntimeException("Missing LazyValue type: " + mType);
    }
  }
}
