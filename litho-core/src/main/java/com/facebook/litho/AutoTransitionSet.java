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

import com.facebook.litho.internal.ArraySet;

/**
 * A set of {@link AutoTransition}s.
 */
public class AutoTransitionSet {

  private final ArrayList<AutoTransition> mAutoTransitions = new ArrayList<>();
  private final ArraySet<String> mKeysWithDisappearAnimations = new ArraySet<>();

  AutoTransitionSet(AutoTransition.Builder... transitionBuilders) {
    for (int i = 0; i < transitionBuilders.length; i++) {
      mAutoTransitions.addAll(transitionBuilders[i].getTransitions());
    }
  }

  boolean hasDisappearAnimation(String key) {
    return mKeysWithDisappearAnimations.contains(key);
  }

  ArrayList<AutoTransition> getAutoTransitions() {
    return mAutoTransitions;
  }

  void mergeIn(AutoTransitionSet set) {
    mAutoTransitions.addAll(set.mAutoTransitions);
    mKeysWithDisappearAnimations.addAll(set.mKeysWithDisappearAnimations);
  }

  void clear() {
    mAutoTransitions.clear();
    mKeysWithDisappearAnimations.clear();
  }
}
