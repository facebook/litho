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

  public static TransitionSet createSet(Transition.Builder ... transitionBuilders) {
    if (transitionBuilders == null || transitionBuilders.length == 0) {
      throw new IllegalArgumentException("You need to define at least a transition within a Set");
    }

    return new TransitionSet(transitionBuilders);
  }

  String getKey() {
    return mKey;
  }

  @TransitionType int getTransitionType() {
    return mTransitionType;
  }

  @PropertyType
  int getValuesFlag() {
    return mValuesFlag;
  }

  PropertySetHolder getLocalValues() {
    return mLocalValues;
  }

  void start(View view, PropertySetHolder startValues, PropertySetHolder endValues) {
    mAnimator.start(
        view,
        TransitionProperties.createPropertyChangeHolderList(
            startValues,
            endValues,
            getValuesFlag()));
  }

  /**
   * Stop the running transition, set the end values to the current target View and save the
   * animation time to restore it if needed.
   */
  void stop() {
    mAnimator.stop();
  }

  boolean restoreState(Transition transition) {
    return mAnimator.restoreState(transition.mAnimator);
  }

  void setListener(TransitionListener listener) {
    mAnimator.setListener(listener);
  }

  public static class Builder {

    private final String mKey;
    private final Transition mTransition;

    private Transition mAppearTransition;
    private Transition mDisappearTransition;
    private @PropertyType int mCurrentEvaluatingType = PropertyType.NONE;

    Builder(String key) {
      mKey = key;
      mTransition = new Transition(key, CHANGE);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    Builder(String key, TransitionAnimator animator) {
      mKey = key;
      mTransition = new Transition(key, CHANGE, animator);
    }

    /**
     * Animate the alpha property of the object backed by the matching transitionKey.
     */
    public Builder alpha() {
      mCurrentEvaluatingType = PropertyType.ALPHA;
      mTransition.mValuesFlag |= PropertyType.ALPHA;
      return this;
    }

    /**
     * Animate the translationX property of the object backed by the matching transitionKey.
     */
    public Builder translationX() {
      mCurrentEvaluatingType = PropertyType.TRANSLATION_X;
      mTransition.mValuesFlag |= PropertyType.TRANSLATION_X;
      return this;
    }

    /**
     * Animate the translationY property of the object backed by the matching transitionKey.
     */
    public Builder translationY() {
      mCurrentEvaluatingType = PropertyType.TRANSLATION_Y;
      mTransition.mValuesFlag |= PropertyType.TRANSLATION_Y;
      return this;
    }

    /**
     * Set the start value to animate the previous property from,
     * when the component will appear on screen.
     */
    public Builder withStartValue(float startValue) {
      if (mAppearTransition == null) {
        mAppearTransition = new Transition(mKey, APPEAR);
      }

      setValue(mAppearTransition, mCurrentEvaluatingType, startValue);
      return this;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    Builder withStartValue(float startValue, TransitionAnimator animator) {
      if (mAppearTransition == null) {
        mAppearTransition = new Transition(mKey, APPEAR, animator);
      }

      setValue(mAppearTransition, mCurrentEvaluatingType, startValue);
      return this;
    }

    /**
     * Set the end value to animate the previous property to,
     * when the component will disappear from the screen.
     */
    public Builder withEndValue(float endValue) {
      if (mDisappearTransition == null) {
        mDisappearTransition = new Transition(mKey, DISAPPEAR);
      }

      setValue(mDisappearTransition, mCurrentEvaluatingType, endValue);
      return this;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Builder withEndValue(float endValue, TransitionAnimator animator) {
      if (mDisappearTransition == null) {
        mDisappearTransition = new Transition(mKey, DISAPPEAR, animator);
      }

      setValue(mDisappearTransition, mCurrentEvaluatingType, endValue);
      return this;
    }

    /**
     * Define an animator to be use in case of a change transition. If not separately defined,
     * this animator will also be used for appear and disappear transitions.
     * {@link TransitionInterpolatorAnimator} is the default animator able to run interpolators.
     */
    public Builder animator(TransitionAnimator animator) {
      mTransition.mAnimator = animator;
      return this;
    }

    /**
     * Define an animator to be use in appear transitions.
     * {@link TransitionInterpolatorAnimator} is the default animator able to run interpolators.
     */
    public Builder animatorOnAppear(TransitionAnimator animator) {
      if (mAppearTransition == null) {
        mAppearTransition = new Transition(mKey, APPEAR);
      }
      mAppearTransition.mAnimator = animator;
      return this;
    }

    /**
     * Define an animator to be use in disappear transitions.
     * {@link TransitionInterpolatorAnimator} is the default animator able to run interpolators.
     */
    public Builder animatorOnDisappear(TransitionAnimator animator) {
      if (mDisappearTransition == null) {
        mDisappearTransition = new Transition(mKey, DISAPPEAR);
      }
      mDisappearTransition.mAnimator = animator;
      return this;
    }

    public Transition build() {
      if (mTransition.mAnimator == null) {
        mTransition.mAnimator = new TransitionInterpolatorAnimator();
      }

      if (mAppearTransition != null && mAppearTransition.mAnimator == null) {
        mAppearTransition.mAnimator = mTransition.mAnimator.clone();
      }

      if (mDisappearTransition != null && mDisappearTransition.mAnimator == null) {
        mDisappearTransition.mAnimator = mTransition.mAnimator.clone();
      }

      if (mAppearTransition != null && mDisappearTransition != null) {
        return TransitionSet.createSet(mTransition, mAppearTransition, mDisappearTransition);
      }
      if (mAppearTransition != null && mDisappearTransition == null) {
        return TransitionSet.createSet(mTransition, mAppearTransition);
      }
      if (mAppearTransition == null && mDisappearTransition != null) {
        return TransitionSet.createSet(mTransition, mDisappearTransition);
      }

      return mTransition;
    }

    private static void setValue(Transition transition, @PropertyType int type, float value) {
      if (type == PropertyType.NONE) {
        throw new IllegalStateException("Before setting a start/end transition value, you should " +
            "define with transition type you want to animate.");
      }

      transition.mValuesFlag |= type;
      transition.mLocalValues.set(type, value);
    }
  }
}
