/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.View;

import com.facebook.litho.TransitionProperties.PropertyChangeHolder;
import com.facebook.litho.TransitionProperties.PropertySetHolder;
import com.facebook.litho.TransitionProperties.PropertyType;

import static com.facebook.litho.Transition.TransitionType.APPEAR;
import static com.facebook.litho.Transition.TransitionType.CHANGE;
import static com.facebook.litho.Transition.TransitionType.DISAPPEAR;
import static com.facebook.litho.Transition.TransitionType.UNDEFINED;

/**
 * A Transition is an animation running on a Component or ComponentLayout with the matching
 * transitionKey {@link com.facebook.litho.ComponentLayout.Builder#transitionKey(String)}.
 */
public class Transition {

  interface TransitionListener {
    void onTransitionEnd();
  }

  interface TransitionAnimator<T extends TransitionAnimator<? super T>> extends Cloneable {
    void setListener(TransitionListener listener);
    void start(View targetView, List<PropertyChangeHolder> propertyChangeHolders);
    T stop();
    /**
     * Restore state from previous transition. If this type of animator cannot restore its state it
     * should return false.
     *
     * @return whether this animator can restore state.
     */
    boolean restoreState(T savedBundle);
    TransitionAnimator clone();
  }

  @IntDef({UNDEFINED, APPEAR, CHANGE, DISAPPEAR})
  @Retention(RetentionPolicy.SOURCE)
  @interface TransitionType {
    int UNDEFINED = -1;
    int APPEAR = 0;
    int CHANGE = 1;
    int DISAPPEAR = 2;
  }

  private final String mKey;
  private final @TransitionType int mTransitionType;
  private final PropertySetHolder mLocalValues;

  private TransitionAnimator mAnimator;
  private @PropertyType int mValuesFlag = PropertyType.NONE;

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  Transition(String key, @TransitionType int transitionType) {
    this(key, transitionType, null);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  private Transition(String key, @TransitionType int transitionType, TransitionAnimator animator) {
    mKey = key;
    mTransitionType = transitionType;
    mAnimator = animator;
    mLocalValues = (transitionType == APPEAR || transitionType == DISAPPEAR)
        ? new PropertySetHolder()
        : null;
  }

  /**
   * Create a Transition that will run when the Component/ComponentLayout with the matching
   * transitionKey will be mounted on screen.
   */
  public static Transition.Builder create(String key) {
    if (TextUtils.isEmpty(key)) {
      throw new IllegalArgumentException("You need to define a key for an OnAppear transition");
    }

    return new Builder(key);
  }

  /**
   * Create a set of {@link Transition} to be return by the @OnLayoutTransition lifecycle method.
   */
  public static TransitionSet createSet(Transition... transitions) {
    if (transitions == null || transitions.length == 0) {
      throw new IllegalArgumentException("You need to define at least a transition within a Set");
    }

    return new TransitionSet(transitions);
  }

