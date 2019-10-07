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
import com.facebook.litho.animation.SequenceBinding;
import java.util.List;

/** A {@link TransitionSet} that runs its child transitions in sequence, one after another. */
public class SequenceTransitionSet extends TransitionSet {

  public <T extends Transition> SequenceTransitionSet(T... children) {
    super(children);
  }

  public <T extends Transition> SequenceTransitionSet(List<T> children) {
    super(children);
  }

  @Override
  AnimationBinding createAnimation(List<AnimationBinding> children) {
    return new SequenceBinding(children);
  }
}
