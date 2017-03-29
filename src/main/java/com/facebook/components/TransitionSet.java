/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.animation.Animator;
import android.view.View;

/**
 * Container for a list of {@link Transition}.
 */
public class TransitionSet extends Transition {

  final private List<Transition> mTransitions;

  TransitionSet(Transition... transitions) {
    super(null, TransitionType.UNDEFINED);
    mTransitions = Arrays.asList(transitions);
  }

  TransitionSet(Transition.Builder... transitionBuilders) {
    super(null, TransitionType.UNDEFINED);
    mTransitions = new ArrayList<>();
    for (int i = 0, size = transitionBuilders.length; i < size; i++) {
      mTransitions.add(transitionBuilders[i].build());
    }
  }

  int size() {
    return mTransitions.size();
  }

  Transition get(int location) {
    return mTransitions.get(location);
  }
}
