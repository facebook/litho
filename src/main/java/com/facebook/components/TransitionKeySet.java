// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.annotation.TargetApi;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;

import com.facebook.components.Transition.TransitionListener;
import com.facebook.components.TransitionManager.KeyStatus;
import com.facebook.components.TransitionProperties.PropertySetHolder;
import com.facebook.components.TransitionProperties.PropertyType;

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

    return start(keyStatus, listener, null, null);
  }

  void stop() {
    mInterruptedValues =
        TransitionProperties.createPropertySetHolder(mTrackedValuesFlag, mTargetView);
    for (int i = 0, size = mRunningTransitionsPointer.size(); i < size; i++) {
      // When appearing, the startValues are null and needs to be "corrected".
      Transition t = mRunningTransitionsPointer.valueAt(i);
      t.stop();
    }

    if (mRunningTransitionsPointer.size() > 0) {
      mTransitionKeySetListener.onTransitionKeySetStop(mKey, mTargetView);
    }

    mAnimationRunningCounter = 0;
  }

  /**
   * For disappear transitions we need to reset the state of the view from animation end state
   * to its initial state so that it can be safely used for further recycling.
   */
  private void resetViewPropertiesAfterDisappear() {
    mStartValues.applyProperties(mTargetView);
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

    switch (newKeyStatus) {
      case KeyStatus.UNCHANGED:
        if (mEndValues.equals(oldTransition.mEndValues)) {
          mStartValues = oldTransition.mStartValues;
          if (oldTransition.wasRunningAppearTransition()) {
            return start(
                KeyStatus.APPEARED,
                listener,
                oldTransition.mRunningTransitionsPointer,
                oldTransition.mInterruptedValues);
          } else if (oldTransition.wasRunningChangeTransition()) {
            return start(
                KeyStatus.UNCHANGED,
                listener,
                oldTransition.mRunningTransitionsPointer,
                oldTransition.mInterruptedValues);
          } else {
            throw new IllegalStateException("Trying to resume a transition with an invalid state.");
          }
        }

        // Different endValues, run a new change transition using the Values where the previous
        // one was interrupted.
        return start(KeyStatus.UNCHANGED, listener, null, oldTransition.mInterruptedValues);

      case KeyStatus.DISAPPEARED:
        if (oldTransition.wasRunningDisappearTransition()) {
          // Was disappearing, continue disappearing animation.
          mStartValues = oldTransition.mStartValues;
          mEndValues = mLocalEndValues;
          setTargetView(oldTransition.mTargetView);
          setTransitionCleanupListener(oldTransition.mTransitionCleanupListener);
          if (oldTransition.mEndValues.equals(mLocalEndValues)) {
            return start(
                KeyStatus.DISAPPEARED,
                listener,
                oldTransition.mRunningTransitionsPointer,
                oldTransition.mInterruptedValues);
          } else {
            return start(KeyStatus.DISAPPEARED, listener, null, oldTransition.mInterruptedValues);
          }
        } else if (oldTransition.wasRunningAppearTransition()) {
          // Was appearing now disappearing.
          mStartValues = oldTransition.mEndValues;
          mEndValues = mLocalEndValues;
          return start(KeyStatus.DISAPPEARED, listener, null, oldTransition.mInterruptedValues);
        } else {
          throw new IllegalStateException("Trying to resume a transition with an invalid state.");
        }

      case KeyStatus.APPEARED:
        // If we are resuming from disappear transition to appear transition,
        // we need to make sure to clean up the state of the current transition
        // before it is implicitly de-referenced.
        if (oldTransition.wasRunningDisappearTransition()) {
          oldTransition.cleanupAfterDisappear();
        }

        // Was disappearing and now re-appearing.
        return start(KeyStatus.APPEARED, listener, null, oldTransition.mInterruptedValues);
    }

    return false;
  }

  @Override
  public void onTransitionEnd() {
    if (--mAnimationRunningCounter == 0) {
      mTransitionKeySetListener.onTransitionKeySetEnd(mKey, mTargetView);
      if (mTransitionCleanupListener != null) {
        mTransitionCleanupListener.onTransitionCleanup();
      }
      if (wasRunningDisappearTransition()) {
        resetViewPropertiesAfterDisappear();
      }
    } else if (mAnimationRunningCounter < 0) {
      throw new IllegalStateException("Wrong number of TransitionEnd received.");
    }
  }

  /**
   * Start transitions for a key given its keyStatus. If you stopped half way through previous
   * transitions for this key, you can pass them as an argument and the new transitions will resume
   * from where the old one stopped.
   *
   * @param keyStatus the status of the key to be animated.
   * @param listener listener to be notified when the set of transitions finished.
   * @param oldRunningTransitions previous running transitions to recover from their state.
   * @param interruptedValues interrupted values from previous transition if any which might be used
   *                          instead of start values.
   * @return true if we started any transition.
   */
  private boolean start(
      @KeyStatus int keyStatus,
      TransitionKeySetListener listener,
      SimpleArrayMap<Integer, ? extends Transition> oldRunningTransitions,
      PropertySetHolder interruptedValues) {
    if (oldRunningTransitions == null && areStartEndValuesEqual()) {
      return false;
    }

    mTransitionKeySetListener = listener;
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
          mRunningTransitionsPointer = mDisappearTransitions;
        }
        break;
    }

    if (mRunningTransitionsPointer != null) {
      for (int i = 0, size = mRunningTransitionsPointer.size(); i < size; i++) {
        final Transition t = mRunningTransitionsPointer.valueAt(i);
        final Integer valueFlags = t.getValuesFlag();

        boolean canRestoreState = oldRunningTransitions != null
                && oldRunningTransitions.containsKey(valueFlags);

        // Copy over a previous state of the same transition if possible.
        canRestoreState = canRestoreState && t.restoreState(oldRunningTransitions.get(valueFlags));

        final PropertySetHolder startValues =
            canRestoreState || interruptedValues == null
                ? mStartValues
                : interruptedValues;

        t.start(
            mTargetView,
            startValues,
            mEndValues);
        mAnimationRunningCounter++;
      }

      if (mRunningTransitionsPointer.size() > 0) {
        mTransitionKeySetListener.onTransitionKeySetStart(mKey, mTargetView);
        return true;
      }
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

  boolean hasDisappearingTransitions() {
    return (mDisappearTransitions != null && !mDisappearTransitions.isEmpty());
  }

  private boolean wasRunningAppearTransition() {
    return (mRunningTransitionsPointer == mAppearTransition);
  }

  private boolean wasRunningChangeTransition() {
    return (mRunningTransitionsPointer == mChangeTransitions);
  }

  boolean wasRunningDisappearTransition() {
    return (mRunningTransitionsPointer == mDisappearTransitions);
  }

  void setTransitionCleanupListener(TransitionCleanupListener listener) {
    mTransitionCleanupListener = listener;
  }

  void cleanupAfterDisappear() {
    if (mTransitionCleanupListener != null) {
      mTransitionCleanupListener.onTransitionCleanup();
    }
    resetViewPropertiesAfterDisappear();
  }
}
