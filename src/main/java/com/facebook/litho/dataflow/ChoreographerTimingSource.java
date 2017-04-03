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
 * The default {@link TimingSource} hooked into Android's Choreographer.
 */
public class ChoreographerTimingSource implements TimingSource {

  private final ChoreographerCompat mChoreographerCompat;
  private final ChoreographerCompat.FrameCallback mFrameCallback;
  private DataFlowGraph mDataFlowGraph;
  private boolean mIsRunning = false;
  private boolean mHasPostedFrameCallback = false;

  public ChoreographerTimingSource() {
    mChoreographerCompat = ChoreographerCompat.getInstance();
    mFrameCallback = new ChoreographerCompat.FrameCallback() {
      @Override
      public void doFrame(long frameTimeNanos) {
        ChoreographerTimingSource.this.doFrame(frameTimeNanos);
      }
    };
  }

  public void setDataFlowGraph(DataFlowGraph dataFlowGraph) {
    mDataFlowGraph = dataFlowGraph;
  }

  @Override
  public void start() {
    if (mDataFlowGraph == null) {
      throw new RuntimeException("Must set a binding graph first.");
    }
    if (mIsRunning) {
      throw new RuntimeException("Tried to start but was already running.");
    }
    mIsRunning = true;
    postFrameCallback();
  }

  @Override
  public void stop() {
    if (!mIsRunning) {
      throw new RuntimeException("Tried to stop but wasn't running.");
    }
    mIsRunning = false;
    stopFrameCallback();
  }

  private void postFrameCallback() {
    if (mHasPostedFrameCallback) {
      return;
    }
    mChoreographerCompat.postFrameCallback(mFrameCallback);
    mHasPostedFrameCallback = true;
  }

  private void stopFrameCallback() {
    mChoreographerCompat.removeFrameCallback(mFrameCallback);
  }

  private void doFrame(long frameTimeNanos) {
    mHasPostedFrameCallback = false;
    if (!mIsRunning) {
      return;
    }

    mDataFlowGraph.doFrame(frameTimeNanos);

    if (mIsRunning) {
      postFrameCallback();
    }
  }
}
