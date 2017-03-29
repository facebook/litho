/**
 * Copyright (c) 2014-present, Facebook, Inc.
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
public class SpringNode extends ValueNode<Float> {

  public static final double NS_PER_SECOND = 1000_000_000.;

  private final Spring mSpring;
  private long mLastFrameTimeNs = Long.MIN_VALUE;

  public SpringNode() {
    mSpring = new Spring();
  }

  @Override
  public Float calculateValue(long frameTimeNanos) {
    final ValueNode<Float> endValue = getInput("end");
    mSpring.setEndValue(endValue.getValue());
    if (mLastFrameTimeNs != Long.MIN_VALUE) {
      double timeDeltaSec = (frameTimeNanos - mLastFrameTimeNs) / NS_PER_SECOND;
      mSpring.advance(timeDeltaSec);
    }
    mLastFrameTimeNs = frameTimeNanos;
    return (float) mSpring.getCurrentValue();
  }

  @Override
  public Float initialize() {
    float initialValue = (Float) getInput("initial").getValue();
    mSpring.setCurrentValue(initialValue);
    return initialValue;
  }
}
