/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.HashSet;

import android.support.v4.util.SimpleArrayMap;

/**
 * TransitionContext is unique per LayoutState and contains all the transitions defined
 * in a component tree.
 */
class TransitionContext {

  // User defined transitions
  private final SimpleArrayMap<String, TransitionKeySet> mKeyToTransitionKeySets =
      new SimpleArrayMap<>();

  // Transition keys of given layout tree
  private final HashSet<String> mTransitionKeys = new HashSet<>(8);

  void add(Transition transition) {
    if (transition instanceof TransitionSet) {
      add((TransitionSet) transition);
    } else {
      final String key = transition.getKey();

      if (!mKeyToTransitionKeySets.containsKey(key)) {
        mKeyToTransitionKeySets.put(key, new TransitionKeySet(key));
      }

      mKeyToTransitionKeySets.get(key).add(transition);
    }
  }

  void add(TransitionSet transitions) {
    for (int i = 0, size = transitions.size(); i < size; i++) {
      add(transitions.get(i));
    }
  }

  SimpleArrayMap<String, TransitionKeySet> getTransitionKeySets() {
    return mKeyToTransitionKeySets;
  }

  void reset() {
    mKeyToTransitionKeySets.clear();
