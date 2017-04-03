// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A property on a mount item (View or Drawable), e.g. x, y, scale, text color, etc.
 */
public interface AnimatedProperty {

  /**
   * @return the name of this property. It should be unique across properties that a mount item can
   * have. It's used for identification purposes only. E.g., "x", "y", etc.
   */
  String getName();

  /**
   * @return the current value of this property on the given mount item (View or Drawable).
   */
  float get(Object mountItem);

  /**
   * Updates the value of this property on the given mount item to the given value.
   */
  void set(Object mountItem, float value);
}
