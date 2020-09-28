/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.dataflow;

import com.facebook.litho.choreographercompat.ChoreographerCompat;
import com.facebook.litho.choreographercompat.ChoreographerCompatImpl;

/** The default {@link TimingSource} hooked into Android's Choreographer. */
public class ChoreographerTimingSource implements TimingSource {

  private final ChoreographerCompat mChoreographerCompat;
  private final ChoreographerCompat.FrameCallback mFrameCallback;
  private DataFlowGraph mDataFlowGraph;
  private boolean mIsRunning = false;
  private boolean mHasPostedFrameCallback = false;
  private long mLastFrameTime = Long.MIN_VALUE;

  public ChoreographerTimingSource() {
    mChoreographerCompat = ChoreographerCompatImpl.getInstance();
    mFrameCallback =
        new ChoreographerCompat.FrameCallback() {
          @Override
          public void doFrame(long frameTimeNanos) {
            ChoreographerTimingSource.this.doFrame(frameTimeNanos);
          }
        };
  }

  @Override
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
    mHasPostedFrameCallback = false;
  }

  private void doFrame(long frameTimeNanos) {
    mHasPostedFrameCallback = false;
    if (!mIsRunning) {
      return;
    }

    // Sometimes Choreographer can call doFrame multiple times with the same frame time, especially
    // in the case of skipped frames. De-bounce it here.
    if (mLastFrameTime != frameTimeNanos) {
      mDataFlowGraph.doFrame(frameTimeNanos);
      mLastFrameTime = frameTimeNanos;
    }

    if (mIsRunning) {
      postFrameCallback();
    }
  }
}
