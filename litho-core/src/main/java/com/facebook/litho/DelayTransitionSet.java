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

package com.facebook.litho;

import com.facebook.litho.animation.AnimationBinding;
import com.facebook.litho.animation.DelayBinding;
import java.util.List;

/**
 * A {@link TransitionSet} that, however, designed to have exactly one transition child to which the
 * specified apply would be applied
 */
public class DelayTransitionSet extends TransitionSet {

  private final int mDelayMs;

  public <T extends Transition> DelayTransitionSet(int delayMs, T transition) {
    super(transition);
    mDelayMs = delayMs;
  }

  @Override
  AnimationBinding createAnimation(List<AnimationBinding> children) {
    if (children.size() != 1) {
      throw new IllegalArgumentException(
          "DelayTransitionSet is expected to have exactly one child, provided=" + children);
    }
    return new DelayBinding(mDelayMs, children.get(0));
  }
}
