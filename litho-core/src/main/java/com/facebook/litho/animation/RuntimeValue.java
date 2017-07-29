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
 * Class that represents a float value that may need to be resolved at runtime (since it's relative
 * to a property on a mount content for example).
 */
public interface RuntimeValue {

  /**
   * Uses the given {@link Resolver} to determine the runtime value based on the given
   * {@link ComponentProperty}.
   */
  float resolve(Resolver resolver, PropertyHandle propertyHandle);
}
