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
 * A node that passes through its input value to its outputs.
 */
public class SimpleNode<T> extends ValueNode<T> {

  @Override
  public T calculateValue(long frameTimeNanos) {
    return (T) getInput().getValue();
  }

  @Override
  public T initialize() {
    return (T) getInput().getValue();
  }
}
