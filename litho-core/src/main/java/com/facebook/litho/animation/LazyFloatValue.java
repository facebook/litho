// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A {@link LazyValue} for a float property like scale or alpha.
 */
public class LazyFloatValue implements LazyValue {

  private final float mValue;

  public LazyFloatValue(float value) {
    mValue = value;
  }

  @Override
  public float resolve(Resolver resolver, ComponentProperty componentProperty) {
    return mValue;
  }
}
