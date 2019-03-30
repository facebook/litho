/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.dataflow;

import androidx.core.util.Pair;
import com.facebook.litho.choreographercompat.ChoreographerCompat;
import java.util.ArrayList;

/** TimingSource and Choreographer implementation that allows manual stepping by frame in tests. */
public class MockTimingSource implements TimingSource, ChoreographerCompat {

  public static int FRAME_TIME_MS = 16;

  private static final long FRAME_TIME_NANOS = (long) (FRAME_TIME_MS * 1e6);

  private final ArrayList<Pair<FrameCallback, Long>> mChoreographerCallbacksToStartTimes =
      new ArrayList<>();
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
      fireChoreographerCallbacks();
    }
  }

  private void fireChoreographerCallbacks() {
    int size = mChoreographerCallbacksToStartTimes.size();
    for (int i = 0; i < size; i++) {
      final Pair<FrameCallback, Long> entry = mChoreographerCallbacksToStartTimes.get(i);
      if (entry.second <= mCurrentTimeNanos) {
        entry.first.doFrame(mCurrentTimeNanos);
        mChoreographerCallbacksToStartTimes.remove(i);
        i--;
        size--;
      }
    }
  }

  @Override
  public void postFrameCallback(FrameCallback callbackWrapper) {
    postFrameCallbackDelayed(callbackWrapper, 0);
  }

  @Override
  public void postFrameCallbackDelayed(FrameCallback callbackWrapper, long delayMillis) {
    mChoreographerCallbacksToStartTimes.add(
        new Pair<>(callbackWrapper, (long) (mCurrentTimeNanos + delayMillis * 1e6)));
  }

  @Override
  public void removeFrameCallback(FrameCallback callbackWrapper) {
    for (int i = mChoreographerCallbacksToStartTimes.size() - 1; i >= 0; i--) {
      final Pair<FrameCallback, Long> entry = mChoreographerCallbacksToStartTimes.get(i);
      if (entry.first == callbackWrapper) {
        mChoreographerCallbacksToStartTimes.remove(i);
      }
    }
  }
}
