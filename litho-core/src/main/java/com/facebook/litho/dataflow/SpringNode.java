/**
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
public class SpringNode extends ValueNode<Float> implements NodeCanFinish {

  public static final double NS_PER_SECOND = 1000_000_000.;

  private final Spring mSpring;
  private long mLastFrameTimeNs = Long.MIN_VALUE;
  private boolean mAreParentsFinished = false;
  private boolean mIsFinished = false;

  public SpringNode() {
    mSpring = new Spring();
  }

  @Override
  public Float calculateValue(long frameTimeNanos) {
    if (mLastFrameTimeNs == Long.MIN_VALUE) {
      mLastFrameTimeNs = frameTimeNanos;
      float initialValue = (Float) getInput("initial").getValue();
      final float endValue = (Float) getInput("end").getValue();
      mSpring.setCurrentValue(initialValue);
      mSpring.setEndValue(endValue);
      return initialValue;
    }

    final float endValue = (Float) getInput("end").getValue();
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
    mIsFinished = mSpring.isAtRest();
  }
}
