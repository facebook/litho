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
 * A set of {@link Transition}s.
 */
public class TransitionSet {

  private final ArrayList<Transition> mTransitions = new ArrayList<>();

  TransitionSet(Transition.Builder... transitionBuilders) {
    for (int i = 0; i < transitionBuilders.length; i++) {
      mTransitions.addAll(transitionBuilders[i].getTransitions());
    }
  }

  ArrayList<Transition> getTransitions() {
    return mTransitions;
  }

  void mergeIn(TransitionSet set) {
    mTransitions.addAll(set.mTransitions);
  }

  void clear() {
    mTransitions.clear();
  }
}
