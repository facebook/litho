// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

/**
 * Test node that allows setting of its next value.
 */
public class SettableNode extends ValueNode {

  private float mValue;

  public void setValue(float value) {
    mValue = value;
  }

  @Override
  protected float calculateValue(long frameTimeNanos, ValueNode inputNode) {
    return mValue;
  }

  @Override
  protected float initialize(long inputSpec) {
    return mValue;
  }
}
