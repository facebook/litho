// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.facebook.components.ValuesHolder.ValueType;

import static com.facebook.components.Transition.TransitionType.APPEAR;
import static com.facebook.components.Transition.TransitionType.CHANGE;
import static com.facebook.components.Transition.TransitionType.DISAPPEAR;
import static com.facebook.components.Transition.TransitionType.UNDEFINED;

/**
 * A Transition is an animation running on a Component or ComponentLayout with the matching
 * transitionKey {@link com.facebook.components.ComponentLayout.Builder#transitionKey(String)}.
 */
public class Transition {

  interface TransitionListener {
    void onTransitionEnd();
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
  private final TransitionAnimator mAnimator;
  private final ValuesHolder mLocalValues;

  private @ValueType int mValuesFlag = ValueType.NONE;

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  Transition(String key, @TransitionType int transitionType) {
    this(key, transitionType, new TransitionAnimator());
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  private Transition(String key, @TransitionType int transitionType, TransitionAnimator animator) {
    mKey = key;
    mTransitionType = transitionType;
    mAnimator = animator;
    mLocalValues = (transitionType == APPEAR || transitionType == DISAPPEAR)
        ? new ValuesHolder()
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

  String getKey() {
    return mKey;
  }

  @TransitionType int getTransitionType() {
    return mTransitionType;
  }

  @ValueType int getValuesFlag() {
    return mValuesFlag;
  }

  ValuesHolder getLocalValues() {
    return mLocalValues;
  }

  void start(View view, ValuesHolder startValues, ValuesHolder endValues) {
    mAnimator.start(
        view,
        ValuesHolder.createPropertyValuesHolders(
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

  void restoreState(Transition transition) {
    mAnimator.restoreState(transition.mAnimator);
  }

  void setListener(TransitionListener listener) {
    mAnimator.setListener(listener);
  }

  public static class Builder {

    private final String mKey;
    private final Transition mTransition;

    private Transition mAppearTransition;
    private Transition mDisapperTransition;
    private @ValueType int mCurrentEvaluatingType = ValueType.NONE;
    private int mDuration = -1;
    private int mStartDelay = -1;
    private Interpolator mInterpolator;

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
     * Default interpolator used for this Transition.
     * If not set, an {@link AccelerateDecelerateInterpolator} will be used.
     */
    public Builder interpolator(Interpolator interpolator) {
      mInterpolator = interpolator;
      return this;
    }

    /**
     * Default duration used for this Transition.
     * If not set, the value of 300ms will be used.
     */
    public Builder duration(int duration) {
      mDuration = duration;
      return this;
    }

    /**
     * Default startDelay used for this Transition.
     */
    public Builder startDelay(int delay) {
      mStartDelay = delay;
      return this;
    }

    /**
     * Animate the alpha property of the object backed by the matching transitionKey.
     */
    public Builder alpha() {
      mCurrentEvaluatingType = ValueType.ALPHA;
      mTransition.mValuesFlag |= ValueType.ALPHA;
      return this;
    }

    /**
     * Animate the translationX property of the object backed by the matching transitionKey.
     */
    public Builder translationX() {
      mCurrentEvaluatingType = ValueType.TRANSLATION_X;
      mTransition.mValuesFlag |= ValueType.TRANSLATION_X;
      return this;
    }

    /**
     * Animate the translationY property of the object backed by the matching transitionKey.
     */
    public Builder translationY() {
      mCurrentEvaluatingType = ValueType.TRANSLATION_Y;
      mTransition.mValuesFlag |= ValueType.TRANSLATION_Y;
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
      if (mDisapperTransition == null) {
        mDisapperTransition = new Transition(mKey, DISAPPEAR);
      }

      setValue(mDisapperTransition, mCurrentEvaluatingType, endValue);
      return this;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public Builder withEndValue(float endValue, TransitionAnimator animator) {
      if (mDisapperTransition == null) {
        mDisapperTransition = new Transition(mKey, DISAPPEAR, animator);
      }

      setValue(mDisapperTransition, mCurrentEvaluatingType, endValue);
      return this;
    }

    public Transition build() {
      if (mDuration > 0) {
        mTransition.mAnimator.setDuration(mDuration);
        if (mAppearTransition != null) {
          mAppearTransition.mAnimator.setDuration(mDuration);
        }
        if (mDisapperTransition != null) {
          mDisapperTransition.mAnimator.setDuration(mDuration);
        }
      }

      if (mStartDelay > 0) {
        mTransition.mAnimator.setStartDelay(mStartDelay);
        if (mAppearTransition != null) {
          mAppearTransition.mAnimator.setStartDelay(mStartDelay);
        }
        if (mDisapperTransition != null) {
          mDisapperTransition.mAnimator.setStartDelay(mStartDelay);
        }
      }

      if (mInterpolator != null) {
        mTransition.mAnimator.setInterpolator(mInterpolator);
        if (mAppearTransition != null) {
          mAppearTransition.mAnimator.setInterpolator(mInterpolator);
        }
        if (mDisapperTransition != null) {
          mDisapperTransition.mAnimator.setInterpolator(mInterpolator);
        }
      }

      if (mAppearTransition != null && mDisapperTransition != null) {
        return TransitionSet.createSet(mTransition, mAppearTransition, mDisapperTransition);
      }
      if (mAppearTransition != null && mDisapperTransition == null) {
        return TransitionSet.createSet(mTransition, mAppearTransition);
      }
      if (mAppearTransition == null && mDisapperTransition != null) {
        return TransitionSet.createSet(mTransition, mDisapperTransition);
      }

      return mTransition;
    }

    private static void setValue(Transition transition, @ValueType int type, float value) {
      if (type == ValueType.NONE) {
        throw new IllegalStateException("Before setting a start/end transition value, you should " +
            "define with transition type you want to animate.");
      }

      transition.mValuesFlag |= type;
      transition.mLocalValues.set(type, value);
    }
  }
}
