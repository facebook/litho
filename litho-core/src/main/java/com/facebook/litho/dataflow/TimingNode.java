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
  private boolean mAreParentsFinished = false;
  private boolean mIsFinished = false;

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
      mIsFinished = true;
      return END_VALUE;
    }

    mLastValueTimeNs = frameTimeNanos;
    return (float) (frameTimeNanos - mStartTimeNs) / (mExpectedEndTimeNs - mStartTimeNs);
  }

  @Override
  public boolean isFinished() {
    return mIsFinished && mAreParentsFinished;
  }

  @Override
  public void onInputsFinished() {
    mAreParentsFinished = true;
    mIsFinished = mLastValueTimeNs >= mExpectedEndTimeNs;
  }
}
