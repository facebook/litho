/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow;

import com.facebook.litho.dataflow.springs.Spring;

/**
 * A node that implements spring physics: it takes an initial value ("initial" input) and
 * end value ("end" input) and animates that value on each frame, outputting the progress over time.
 */
public class SpringNode extends ValueNode implements NodeCanFinish {

  public static final double NS_PER_SECOND = 1000_000_000.;
  public static final String INITIAL_INPUT = "initial";
  public static final String END_INPUT = "end";

  private final Spring mSpring;
  private long mLastFrameTimeNs = Long.MIN_VALUE;
  private boolean mAreParentsFinished = false;

  public SpringNode() {
    mSpring = new Spring();
  }

  @Override
  public float calculateValue(long frameTimeNanos) {
    if (mLastFrameTimeNs == Long.MIN_VALUE) {
      mLastFrameTimeNs = frameTimeNanos;
      float initialValue = getInput(INITIAL_INPUT).getValue();
      final float endValue = getInput(END_INPUT).getValue();
      mSpring.setCurrentValue(initialValue);
      mSpring.setEndValue(endValue);
      return initialValue;
    }

    final float endValue = getInput(END_INPUT).getValue();
    mSpring.setEndValue(endValue);
    if (isFinished()) {
      return endValue;
    }

    double timeDeltaSec = (frameTimeNanos - mLastFrameTimeNs) / NS_PER_SECOND;
    mSpring.advance(timeDeltaSec);
    mLastFrameTimeNs = frameTimeNanos;

    return (float) mSpring.getCurrentValue();
  }

  @Override
  public boolean isFinished() {
    return mAreParentsFinished && mSpring.isAtRest();
  }

  @Override
  public void onInputsFinished() {
    mAreParentsFinished = true;
  }
}
