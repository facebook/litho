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

/**
 * A {@link ValueNode} that will linearly update its value from 0 to 1.0 over the course of the
 * given duration.
 */
public class TimingNode extends ValueNode implements NodeCanFinish {

  private static final int MS_IN_NANOS = 1000000;
  private static final float INITIAL_VALUE = 0.0f;
  private static final float END_VALUE = 1.0f;

  private final long mDurationMs;
  private long mStartTimeNs = Long.MIN_VALUE;
  private long mExpectedEndTimeNs = Long.MIN_VALUE;
  private long mLastValueTimeNs = Long.MIN_VALUE;

  public TimingNode(int durationMs) {
    mDurationMs = durationMs;
  }

  @Override
  public float calculateValue(long frameTimeNanos) {
    if (mLastValueTimeNs == Long.MIN_VALUE) {
      mStartTimeNs = frameTimeNanos;
      mLastValueTimeNs = frameTimeNanos;
      mExpectedEndTimeNs = mStartTimeNs + (mDurationMs * MS_IN_NANOS);
      return INITIAL_VALUE;
    }

    if (frameTimeNanos >= mExpectedEndTimeNs) {
      mLastValueTimeNs = frameTimeNanos;
      return END_VALUE;
    }

    mLastValueTimeNs = frameTimeNanos;
    return (float) (frameTimeNanos - mStartTimeNs) / (mExpectedEndTimeNs - mStartTimeNs);
  }

  @Override
  public boolean isFinished() {
    return mLastValueTimeNs >= mExpectedEndTimeNs;
  }
}
