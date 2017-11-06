/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.dataflow;

import android.view.animation.Interpolator;

/**
 * A {@link ValueNode} that will update its value by mapping an input value between 0 to 1.0
 * representing elapsed fraction of animation to a value that represents interpolated fraction.
 */
public class InterpolatorNode extends ValueNode {

  private final Interpolator mInterpolator;

  public InterpolatorNode(Interpolator interpolator) {
    mInterpolator = interpolator;
  }

  @Override
  protected float calculateValue(long frameTimeNanos) {
    float timingValue = getInput(DEFAULT_INPUT).getValue();
    return mInterpolator.getInterpolation(timingValue);
  }
}
