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
 * A {@link RuntimeValue} for a float property like scale or alpha.
 */
public class FloatValue implements RuntimeValue {

  private final float mValue;

  public FloatValue(float value) {
    mValue = value;
  }

  @Override
  public float resolve(Resolver resolver, ComponentProperty componentProperty) {
    return mValue;
  }
}
