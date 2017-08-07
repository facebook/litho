/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import java.util.List;

import com.facebook.litho.animation.AnimationBinding;
import com.facebook.litho.animation.SequenceBinding;

/**
 * A {@link TransitionSet} that runs its child transitions in sequence, one after another.
 */
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
