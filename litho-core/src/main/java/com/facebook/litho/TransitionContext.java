/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.ArrayList;

/**
 * TransitionContext is unique per LayoutState and contains all the transitions defined
 * in a component tree.
 */
class TransitionContext {

  private final ArrayList<Transition> mTransitions = new ArrayList<>();

  void addTransition(Transition transition) {
    if (transition instanceof Transition.TransitionUnitsBuilder) {
      mTransitions.addAll(((Transition.TransitionUnitsBuilder) transition).getTransitionUnits());
    } else {
      mTransitions.add(transition);
    }
  }

  ArrayList<Transition> getTransitions() {
    return mTransitions;
  }

  void reset() {
    mTransitions.clear();
  }
}
