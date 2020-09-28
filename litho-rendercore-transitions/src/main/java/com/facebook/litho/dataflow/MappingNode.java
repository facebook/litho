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
