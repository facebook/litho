/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.dataflow;

/**
 * Multi-input node for unit tests
 */
public class AdditionNode extends ValueNode<Float> {

  @Override
  protected Float calculateValue(long frameTimeNanos) {
    return (Float) getInput("a").getValue() + (Float) getInput("b").getValue();
  }

  @Override
  protected Float initialize() {
    return 0f;
  }
}
