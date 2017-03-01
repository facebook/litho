// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.v4.util.SimpleArrayMap;

/**
 * TransitionContext is unique per LayoutState and contains all the transitions defined
 * in a component tree.
 */
class TransitionContext {

  private final SimpleArrayMap<String, TransitionKeySet> mKeyToTransitionKeySets =
      new SimpleArrayMap<>();

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
  }
}
