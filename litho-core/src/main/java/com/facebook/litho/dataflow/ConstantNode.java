/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow;

/**
 * A dataflow node that returns a constant value.
 */
public class ConstantNode extends ValueNode {

  private final float mValue;

  public ConstantNode(float value) {
    mValue = value;
  }

  @Override
  public float calculateValue(long frameTimeNanos) {
    return mValue;
  }
}
