/*
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
import java.util.List;

/**
 * A set of {@link Transition}s.
 */
public abstract class TransitionSet extends Transition {

  private final ArrayList<Transition> mChildren = new ArrayList<>();

  <T extends Transition> TransitionSet(T... children) {
    for (int i = 0; i < children.length; i++) {
      addChild(children[i]);
    }
  }

  <T extends Transition> TransitionSet(List<T> children) {
    for (int i = 0; i < children.size(); i++) {
      addChild(children.get(i));
    }
  }

  private void addChild(Transition child) {
    if (child instanceof Transition.BaseTransitionUnitsBuilder) {
      final ArrayList<? extends Transition> transitions =
          ((Transition.BaseTransitionUnitsBuilder) child).getTransitionUnits();
      if (transitions.size() > 1) {
        mChildren.add(new ParallelTransitionSet(transitions));
      } else {
        mChildren.add(transitions.get(0));
      }
    } else {
      mChildren.add(child);
    }
  }

  ArrayList<Transition> getChildren() {
    return mChildren;
  }

  abstract AnimationBinding createAnimation(List<AnimationBinding> children);
}
