// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

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

  int size() {
    return mTransitions.size();
  }

  Transition get(int location) {
    return mTransitions.get(location);
  }
}
