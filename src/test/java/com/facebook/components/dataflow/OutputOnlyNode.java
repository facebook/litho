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
 * Test node that only serves as an output node.
 */
public class OutputOnlyNode extends ValueNode<Float> {

  private final float mInitialValue;

  public OutputOnlyNode(float initialValue) {
    mInitialValue = initialValue;
  }

  @Override
  protected Float calculateValue(long frameTimeNanos) {
    return (Float) getInput().getValue();
  }

  @Override
  protected Float initialize() {
    return mInitialValue;
  }
}
