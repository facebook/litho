/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.animation.AnimationBinding;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * TransitionContext is unique per LayoutState and contains all the transitions defined
 * in a component tree.
 */
class TransitionContext {

  private final ArrayList<AnimationBinding> mTransitionAnimationBindings = new ArrayList<>();
  private final TransitionSet mTransitionSets = new TransitionSet();

  // Transition keys of given layout tree
  private final HashSet<String> mTransitionKeys = new HashSet<>(8);

  void addTransitionAnimationBinding(AnimationBinding binding) {
    mTransitionAnimationBindings.add(binding);
  }

  void addAutoTransitions(TransitionSet transitionSet) {
    mTransitionSets.mergeIn(transitionSet);
  }

  ArrayList<AnimationBinding> getTransitionAnimationBindings() {
    return mTransitionAnimationBindings;
  }

  TransitionSet getAutoTransitionSet() {
    return mTransitionSets;
  }

  void reset() {
    mTransitionAnimationBindings.clear();
    mTransitionSets.clear();
    mTransitionKeys.clear();
  }

  void addTransitionKey(String transitionKey) {
    mTransitionKeys.add(transitionKey);
  }

  boolean hasTransitionKey(String transitionKey) {
    return mTransitionKeys.contains(transitionKey);
  }

  /**
   * @return Whether item with the given {@param transitionKey} is being removed from layout tree.
   */
  boolean isDisappearingKey(String transitionKey) {
    if (transitionKey == null || mTransitionKeys.contains(transitionKey)) {
      return false;
    }
    return mTransitionSets.hasDisappearAnimation(transitionKey);
  }
}
