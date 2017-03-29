/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.animation.Interpolator;

import com.facebook.litho.Transition.TransitionAnimator;
import com.facebook.litho.Transition.TransitionListener;
import com.facebook.litho.TransitionProperties.PropertyChangeHolder;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

/**
 * This is the Default animation engine used in {@link Transition}s and backed by the
 * Android {@link ObjectAnimator}.
 */
@TargetApi(ICE_CREAM_SANDWICH)
public class TransitionInterpolatorAnimator
    implements TransitionAnimator<TransitionInterpolatorAnimator> {

  private static final int DURATION = 1 << 0;
  private static final int START_DELAY = 1 << 1;
  private static final int INTERPOLATOR = 1 << 2;

  private final ObjectAnimator mAnimator;

  private int mSetFlags = 0;
  private int mDuration;
  private int mStartDelay;
  private Interpolator mInterpolator;
  // This is not exact but an approximation used to compute the remaining startDelay.
  private long mStartTime;
  private long mPlayedTime;
  private TransitionListener mListener;

  public static TransitionInterpolatorAnimator.Builder create() {
    return new Builder();
  }

  TransitionInterpolatorAnimator() {
    mAnimator = new ObjectAnimator();
    mAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        if (mListener != null) {
          mListener.onTransitionEnd();
        }
      }
    });
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  TransitionInterpolatorAnimator(ObjectAnimator objectAnimator, long startTime, long playedTime) {
    mAnimator = objectAnimator;
    mStartTime = startTime;
    mPlayedTime = playedTime;
  }

  void setInterpolator(Interpolator interpolator) {
    mSetFlags |= INTERPOLATOR;
    mInterpolator = interpolator;
  }

  void setDuration(int duration) {
    mSetFlags |= DURATION;
    mDuration = duration;
  }

  void setStartDelay(int delay) {
    mSetFlags |= START_DELAY;
    mStartDelay = delay;
  }

  @Override
  public void setListener(TransitionListener listener) {
    mListener = listener;
  }

  @Override
  public boolean restoreState(TransitionInterpolatorAnimator interpolatorAnimator) {
    mStartTime = interpolatorAnimator.mStartTime;
    mPlayedTime = interpolatorAnimator.mPlayedTime;
    return true;
