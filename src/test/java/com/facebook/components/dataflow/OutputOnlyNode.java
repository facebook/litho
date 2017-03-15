// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

/**
 * Test node that only serves as an output node.
 */
public class OutputOnlyNode extends ValueNode {

  private final float mInitialValue;

  public OutputOnlyNode(float initialValue) {
    mInitialValue = initialValue;
  }

  @Override
  protected float calculateValue(long frameTimeNanos, ValueNode inputNode) {
    return inputNode.getValue();
  }

  @Override
  protected float initialize(long inputSpec) {
    return mInitialValue;
  }
}
