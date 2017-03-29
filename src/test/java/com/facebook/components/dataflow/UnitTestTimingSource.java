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
 * TimingSource that allows manual stepping by frame in tests.
 */
public class UnitTestTimingSource implements TimingSource {

  private static final long FRAME_TIME_NANOS = (long) (16 * 1e9);

  private DataFlowGraph mDataFlowGraph;
  private boolean mIsRunning = false;
  private long mCurrentTimeNanos = 0;

  @Override
  public void setDataFlowGraph(DataFlowGraph dataFlowGraph) {
    mDataFlowGraph = dataFlowGraph;
  }

  @Override
  public void start() {
    mIsRunning = true;
  }

  @Override
  public void stop() {
    mIsRunning = false;
  }

  public void step(int numFrames) {
    for (int i = 0; i < numFrames; i++) {
      if (!mIsRunning) {
        return;
      }
      mCurrentTimeNanos += FRAME_TIME_NANOS;
      mDataFlowGraph.doFrame(mCurrentTimeNanos);
    }
  }
}
