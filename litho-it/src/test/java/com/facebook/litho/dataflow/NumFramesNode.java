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
 * Test node whose value is based on the number of frames it's seen.
 */
public class NumFramesNode extends ValueNode implements NodeCanFinish {

  private int mNumFramesSeen = 0;
  private long mLastFrameTime = Long.MIN_VALUE;

  @Override
  protected float calculateValue(long frameTimeNanos) {
    if (mLastFrameTime != frameTimeNanos) {
      mLastFrameTime = frameTimeNanos;
      mNumFramesSeen++;
    }
    return (float) mNumFramesSeen;
  }

  @Override
  public boolean isFinished() {
    return false;
  }

  @Override
  public void onInputsFinished() {
  }
}
