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
 * A {@link ValueNode} that will update its value from its "initial" input to its "end" input
 * according to the given input within expected range [0, 1]. For example {@link MappingNode} could
 * accept values emmited by {@link InterpolatorNode}. The node can also extrapolate beyond the
 * output range if input goes beyond the expected input range.
 */
public class MappingNode extends ValueNode {

  public static final String INITIAL_INPUT = "initial";
  public static final String END_INPUT = "end";

  @Override
  protected float calculateValue(long frameTimeNanos) {
    final float initialValue = getInput(INITIAL_INPUT).getValue();
    final float endValue = getInput(END_INPUT).getValue();
    final float fractionValue = getInput(DEFAULT_INPUT).getValue();

    final float valRange = endValue - initialValue;
    return initialValue + fractionValue * valRange;
  }
}
