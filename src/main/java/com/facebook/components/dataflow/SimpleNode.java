// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

/**
 * A node that passes through its input value to its outputs.
 */
public class SimpleNode extends ValueNode {

  @Override
  public float calculateValue(long frameTimeNanos, ValueNode parent) {
    return parent.getValue();
  }

  @Override
  public float initialize(long inputSpec) {
    return InputSpec.getValue(inputSpec);
  }

  @Override
  public long calculateInputSpec(long inputSpecForOutput) {
    return inputSpecForOutput;
  }
}
