/**
 * Copyright (c) 2014-present, Facebook, Inc.
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
public class ConstantNode<T> extends ValueNode<T> {

  private final T mValue;

  public ConstantNode(T value) {
    mValue = value;
  }

  @Override
  public T calculateValue(long frameTimeNanos) {
    return mValue;
  }

  @Override
  public T initialize() {
    return mValue;
  }
}
