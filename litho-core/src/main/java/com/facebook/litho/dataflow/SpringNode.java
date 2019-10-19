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

import com.facebook.litho.dataflow.springs.Spring;
import com.facebook.litho.dataflow.springs.SpringConfig;

/**
 * A node that implements spring physics: it takes an initial value ("initial" input) and end value
 * ("end" input) and animates that value on each frame, outputting the progress over time.
 */
public class SpringNode extends ValueNode implements NodeCanFinish {

  public static final double NS_PER_SECOND = 1000_000_000.;
  public static final String INITIAL_INPUT = "initial";
  public static final String END_INPUT = "end";

  private final Spring mSpring;
  private long mLastFrameTimeNs = Long.MIN_VALUE;

  public SpringNode() {
    this(null);
  }

  public SpringNode(SpringConfig springConfig) {
    mSpring = new Spring();
    if (springConfig != null) {
      mSpring.setSpringConfig(springConfig);
    }
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
    return mSpring.isAtRest();
  }
}
