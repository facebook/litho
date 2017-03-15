// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

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
