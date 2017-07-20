/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import com.facebook.litho.AnimatableItem;

/**
 * A property on a mount content (View or Drawable), e.g. x, y, scale, text color, etc.
 */
public interface AnimatedProperty {

  /**
   * @return the name of this property. It should be unique across properties that a mount content
   * can have. It's used for identification purposes only. E.g., "x", "y", etc.
   */
  String getName();

  /**
   * @return the current value of this property on the given mount content (View or Drawable).
   */
  float get(Object mountContent);

  /**
   * @return the current value of this property on the given {@link AnimatableItem}.
   */
  float get(AnimatableItem animatableItem);

  /**
   * Updates the value of this property on the given mount content to the given value.
   */
  void set(Object mountContent, float value);

  /**
   * Resets this property to its default value on this mount content.
   */
  void reset(Object mountContent);
}
