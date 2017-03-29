/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.annotation.TargetApi;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;

import com.facebook.litho.Transition.TransitionListener;
import com.facebook.litho.TransitionManager.KeyStatus;
import com.facebook.litho.TransitionProperties.PropertySetHolder;
import com.facebook.litho.TransitionProperties.PropertyType;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.facebook.litho.Transition.TransitionType.APPEAR;
import static com.facebook.litho.Transition.TransitionType.CHANGE;
import static com.facebook.litho.Transition.TransitionType.DISAPPEAR;

/**
 * Holds all the defined {@link Transition}s for a given transition key.
 * It's also responsible to start, stop and resume those transitions on a target View.
 */
// TODO(t16207387): After finished iterating on this class, it needs tests.
@TargetApi(ICE_CREAM_SANDWICH)
class TransitionKeySet implements TransitionListener {

  interface TransitionKeySetListener {
    void onTransitionKeySetStart(String key, View view);
    void onTransitionKeySetStop(String key, View view);
    void onTransitionKeySetEnd(String key, View view);
  }

  /**
   * Callback to clean up the object state associated with this transition when it is finished
   */
  interface TransitionCleanupListener {
    void onTransitionCleanup();
  }

  private final String mKey;

  private SimpleArrayMap<Integer, Transition> mAppearTransition;
  private SimpleArrayMap<Integer, Transition> mChangeTransitions;
  private SimpleArrayMap<Integer, Transition> mDisappearTransitions;
  private SimpleArrayMap<Integer, Transition> mRunningTransitionsPointer;
  // Values of the item to transition, at the beginning of the mount process.
  private PropertySetHolder mStartValues;
  // Values of the item to transition, at the end of the mount process.
  private PropertySetHolder mEndValues;
  // Combined start values from all the appear transitions.
  private PropertySetHolder mLocalStartValues;
  // Combined end values from all the disappear transitions.
  private PropertySetHolder mLocalEndValues;
  // Intermediate values of an item in case it was interrupted before the end of its transition.
  private PropertySetHolder mInterruptedValues;
  // Cumulative ValuesFlag to track across all the transitions for a given key.
  private @PropertyType int mTrackedValuesFlag;
  private View mTargetView;
  private TransitionKeySetListener mTransitionKeySetListener;
  private TransitionCleanupListener mTransitionCleanupListener;
  private int mAnimationRunningCounter = 0;

  TransitionKeySet(String key) {
    mKey = key;
  }

  void setTargetView(View view) {
    mTargetView = view;
  }

  void add(Transition transition) {
    final SimpleArrayMap<Integer, Transition> tmpPointer;

    switch (transition.getTransitionType()) {
      case APPEAR:
        if (mAppearTransition == null) {
          mAppearTransition = new SimpleArrayMap<>();
        }

        if (mLocalStartValues == null) {
          mLocalStartValues = transition.getLocalValues();
        } else {
          mLocalStartValues.addProperties(transition.getLocalValues());
        }

        tmpPointer = mAppearTransition;
        break;

      case CHANGE:
        if (mChangeTransitions == null) {
          mChangeTransitions = new SimpleArrayMap<>();
        }

        tmpPointer = mChangeTransitions;
        break;

      case DISAPPEAR:
        if (mDisappearTransitions == null) {
          mDisappearTransitions = new SimpleArrayMap<>();
        }

        if (mLocalEndValues == null) {
          mLocalEndValues = transition.getLocalValues();
        } else {
          mLocalEndValues.addProperties(transition.getLocalValues());
        }

        tmpPointer = mDisappearTransitions;
        break;

      default:
        throw new IllegalStateException("Transition type not valid for key: "+transition.getKey());
    }

    tmpPointer.put(transition.getValuesFlag(), transition);
    mTrackedValuesFlag |= transition.getValuesFlag();
    transition.setListener(this);
  }

  void recordStartValues(View view) {
    if (view == null) {
      return;
    }

    mStartValues = TransitionProperties.createPropertySetHolder(mTrackedValuesFlag, view);
  }

  void recordEndValues(View view) {
    if (view == null) {
      return;
    }

    mEndValues = TransitionProperties.createPropertySetHolder(mTrackedValuesFlag, view);
  }

  /**
   * Start transitions for this key given the key status.
   *
   * @param keyStatus
   * @param listener
   * @return True if any transition was actually started.
   */
  boolean start(@KeyStatus int keyStatus, TransitionKeySetListener listener) {
    if (keyStatus == KeyStatus.APPEARED) {
      mStartValues = mLocalStartValues;
    } else if (keyStatus == KeyStatus.DISAPPEARED) {
      mEndValues = mLocalEndValues;
    }

