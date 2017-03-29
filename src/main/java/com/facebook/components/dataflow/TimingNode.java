// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.dataflow;

/**
 * A ValueNode that will linearly update its value from its "initial" input to its "end" input
 * over the course of the given duration.
 *
 * This node supports the "end" input changing: it will animate to that new end value over the
 * remaining duration meaning that velocity of the value may change.
 *
 * NB: If the end input changes after the end of the duration, this node will just pass through that
 * new value.
 */
public class TimingNode extends ValueNode<Float> {

  public static final String INITIAL_INPUT = "initial";
  public static final String END_INPUT = "end";

  private static final int MS_IN_NANOS = 1000000;

  private final int mDurationMs;
  private long mStartTimeNs = Long.MIN_VALUE;
  private long mExpectedEndTimeNs = Long.MIN_VALUE;
  private long mLastValueTimeNs = Long.MIN_VALUE;
  private float mInitialValue;
  private boolean mIsFinished = false;

  public TimingNode(int durationMs) {
    mDurationMs = durationMs;
  }

  @Override
  public Float calculateValue(long frameTimeNanos) {
    if (mLastValueTimeNs == Long.MIN_VALUE) {
      mStartTimeNs = frameTimeNanos;
      mLastValueTimeNs = frameTimeNanos;
      mExpectedEndTimeNs = mStartTimeNs + (mDurationMs * MS_IN_NANOS);
      return mInitialValue;
    }

    float endValue = (Float) getInput(END_INPUT).getValue();
    if (mIsFinished) {
      return endValue;
    }

    if (frameTimeNanos >= mExpectedEndTimeNs) {
      mIsFinished = true;
      return endValue;
    }

    float lastValue = getValue();
    float desiredVelocity = (endValue - lastValue) / (mExpectedEndTimeNs - mLastValueTimeNs);
    float increment = desiredVelocity * (frameTimeNanos - mLastValueTimeNs);

    mLastValueTimeNs = frameTimeNanos;

    return lastValue + increment;
  }

  @Override
  public Float initialize() {
    mInitialValue = (Float) getInput(INITIAL_INPUT).getValue();
    return mInitialValue;
  }
}
