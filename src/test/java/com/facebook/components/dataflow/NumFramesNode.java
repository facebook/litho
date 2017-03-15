// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

/**
 * Test node whose value is based on the number of frames it's seen.
 */
public class NumFramesNode extends ValueNode {

  private int mNumFramesSeen = 0;
  private long mLastFrameTime = Long.MIN_VALUE;

  @Override
  protected float calculateValue(long frameTimeNanos, ValueNode inputNode) {
    if (mLastFrameTime != frameTimeNanos) {
      mLastFrameTime = frameTimeNanos;
      mNumFramesSeen++;
    }
    return mNumFramesSeen;
  }

  @Override
  protected float initialize(long inputSpec) {
    return 0;
  }
}
