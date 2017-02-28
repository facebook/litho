// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.annotation.TargetApi;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;

import com.facebook.components.Transition.TransitionListener;
import com.facebook.components.TransitionManager.KeyStatus;
import com.facebook.components.ValuesHolder.ValueType;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.facebook.components.Transition.TransitionType.APPEAR;
import static com.facebook.components.Transition.TransitionType.CHANGE;
import static com.facebook.components.Transition.TransitionType.DISAPPEAR;

/**
 * Holds all the defined {@link Transition}s for a given transition key.
 * It's also responsible to start, stop and resume those transitions on a target View.
 */
// TODO(t16207387): After finished iterating on this class, it needs tests.
@TargetApi(ICE_CREAM_SANDWICH)
class TransitionKeySet implements TransitionListener {

  interface TransitionKeySetListener {
    void onTransitionKeySetEnd(String key);
  }

  private final String mKey;

  private SimpleArrayMap<Integer, Transition> mAppearTransition;
  private SimpleArrayMap<Integer, Transition> mChangeTransitions;
  private SimpleArrayMap<Integer, Transition> mDisappearTransitions;
  private SimpleArrayMap<Integer, Transition> mRunningTransitionsPointer;
  // Values of the item to transition, at the beginning of the mount process.
  private ValuesHolder mStartValues;
  // Values of the item to transition, at the end of the mount process.
  private ValuesHolder mEndValues;
  // Combined start values from all the appear transitions.
  private ValuesHolder mLocalStartValues;
  // Combined end values from all the disappear transitions.
  private ValuesHolder mLocalEndValues;
  // Intermediate values of an item in case it was interrupted before the end of its transition.
  private ValuesHolder mInterruptedValues;
  // Cumulative ValuesFlag to track across all the transitions for a given key.
  private @ValueType int mTrackedValuesFlag;
  private View mTargetView;
  private TransitionKeySetListener mTransitionEndListener;
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
          mLocalStartValues.addValues(transition.getLocalValues());
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
          mLocalEndValues.addValues(transition.getLocalValues());
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

    mStartValues = new ValuesHolder().recordValues(mTrackedValuesFlag, view);
  }

  void recordEndValues(View view) {
    if (view == null) {
      return;
    }

    mEndValues = new ValuesHolder().recordValues(mTrackedValuesFlag, view);
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

    return start(keyStatus, listener, null);
  }

  void stop() {
    mInterruptedValues = new ValuesHolder().recordValues(mTrackedValuesFlag, mTargetView);
    for (int i = 0, size = mRunningTransitionsPointer.size(); i < size; i++) {
      // When appearing, the startValues are null and needs to be "corrected".
      Transition t = mRunningTransitionsPointer.valueAt(i);
      t.stop();
    }

    mAnimationRunningCounter = 0;
  }

  /**
   * Resume transitions, if needed, given the previous running transitions and the new key status.
   *
   * @param oldTransition previously running transitions.
   * @param newKeyStatus
   * @param listener
   * @return true if new transitions started.
   */
  boolean resumeFrom(
      TransitionKeySet oldTransition,
      int newKeyStatus,
      TransitionKeySetListener listener) {
    final boolean hasSameEndValues = mEndValues.equals(oldTransition.mEndValues);

    switch (newKeyStatus) {
      case KeyStatus.UNCHANGED:
        if (hasSameEndValues) {
          mStartValues = oldTransition.mStartValues;
          if (oldTransition.wasRunningAppearTransition()) {
            return start(KeyStatus.APPEARED, listener, oldTransition.mAppearTransition);
          } else if (oldTransition.wasRunningChangeTransition()) {
            return start(KeyStatus.UNCHANGED, listener, oldTransition.mChangeTransitions);
          } else {
            throw new IllegalStateException("Trying to resume a transition with an invalid state.");
          }
        }

        // Different endValues, run a new change transition using the Values where the previous
        // one was interrupted.
        mStartValues = oldTransition.mInterruptedValues;
        return start(KeyStatus.UNCHANGED, listener);

      case KeyStatus.DISAPPEARED:
        // TODO: 1) An appearing or change transition was stopped and the key now disappeared.
        //       2) Can a disappearing transition be resumed? Depends if we retain the key or not.
        return false;
    }

    return false;
  }

  @Override
  public void onTransitionEnd() {
    if (--mAnimationRunningCounter == 0) {
      mTransitionEndListener.onTransitionKeySetEnd(mKey);
    } else if (mAnimationRunningCounter < 0) {
      throw new IllegalStateException("Wrong number of TransitionEnd received.");
    }
  }

  /**
   * Start a transitions for a key given its keyStatus. If you stopped half way through previous
   * transitions for this key, you can pass them as an argument and the new transitions will resume
   * from where the old one stopped.
   *
   * @param keyStatus the status of the key to be animated.
   * @param listener listener to be notified when the set of transitions finished.
   * @param oldRunningTransitions previous running transitions to recover from their state.
   * @return true if we started any transition.
   */
  private boolean start(
      @KeyStatus int keyStatus,
      TransitionKeySetListener listener,
      SimpleArrayMap<Integer, ? extends Transition> oldRunningTransitions) {
    if (oldRunningTransitions == null && areStartEndValuesEqual()) {
      return false;
    }

    mTransitionEndListener = listener;
    mRunningTransitionsPointer = null;
    mAnimationRunningCounter = 0;

    switch (keyStatus) {
      case KeyStatus.APPEARED:
        if (hasAppearingTransitions()) {
          mRunningTransitionsPointer = mAppearTransition;
        }
        break;

      case KeyStatus.UNCHANGED:
        if (hasChangingTransitions()) {
          mRunningTransitionsPointer = mChangeTransitions;
        }
        break;

      case KeyStatus.DISAPPEARED:
        if (hasDisappearingTransitions()) {
          // TODO implementation.
        }
        break;
    }

    if (mRunningTransitionsPointer != null) {
      for (int i = 0, size = mRunningTransitionsPointer.size(); i < size; i++) {
        final Transition t = mRunningTransitionsPointer.valueAt(i);
        final Integer valueFlags = t.getValuesFlag();
        // Copy over a previous state of the same transition if any.
        if (oldRunningTransitions != null && oldRunningTransitions.containsKey(valueFlags)) {
          t.restoreState(oldRunningTransitions.get(valueFlags));
        }

        t.start(mTargetView, mStartValues, mEndValues);
        mAnimationRunningCounter++;
      }

      return true;
    }

    return false;
  }

  private boolean areStartEndValuesEqual() {
    return (mStartValues != null)
        ? mStartValues.equals(mEndValues)
        : mEndValues == null;
  }

  private boolean hasAppearingTransitions() {
    return (mAppearTransition != null && !mAppearTransition.isEmpty());
  }

  private boolean hasChangingTransitions() {
    return (mChangeTransitions != null && !mChangeTransitions.isEmpty());
  }

  private boolean hasDisappearingTransitions() {
    return (mDisappearTransitions != null && !mDisappearTransitions.isEmpty());
  }

  private boolean wasRunningAppearTransition() {
    return (mRunningTransitionsPointer == mAppearTransition);
  }

  private boolean wasRunningChangeTransition() {
    return (mRunningTransitionsPointer == mChangeTransitions);
  }
}
