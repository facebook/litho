// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.animation.Interpolator;

import com.facebook.components.Transition.TransitionListener;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

/**
 * This is the Default animation engine used in {@link Transition}s and backed by the
 * Android {@link ObjectAnimator}.
 */
@TargetApi(ICE_CREAM_SANDWICH)
class TransitionAnimator {

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

  TransitionAnimator() {
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
  TransitionAnimator(ObjectAnimator objectAnimator, long startTime, long playedTime) {
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

  void setListener(TransitionListener listener) {
    mListener = listener;
  }

  void restoreState(TransitionAnimator transitionAnimator) {
    mStartTime = transitionAnimator.mStartTime;
    mPlayedTime = transitionAnimator.mPlayedTime;
  }

  void start(View targetView, PropertyValuesHolder[] propertyValuesHolders) {
    if (mListener == null) {
      throw new IllegalStateException("TransitionAnimator should have a listener set before " +
          "start() is called in order to callback the TransitionManager at the end of the " +
          "animation.");
    }

    mAnimator.setValues(propertyValuesHolders);

    if ((mSetFlags & DURATION) != 0) {
      mAnimator.setDuration(mDuration);
    }
    if ((mSetFlags & INTERPOLATOR) != 0) {
      mAnimator.setInterpolator(mInterpolator);
    }
    // Set startDelay only if the animator has not being restored from a running animation.
    if ((mSetFlags & START_DELAY) != 0 && mPlayedTime == 0) {
      // Adjust the startDelay with the start time of the restored animator.
      if (mStartTime > 0) {
        final long waitedTime = SystemClock.uptimeMillis() - mStartTime;
        mAnimator.setStartDelay(Math.max(0, (mStartDelay - waitedTime)));
        // Remove startDelay if we are resuming with the animation already running.
      } else {
        mAnimator.setStartDelay(mStartDelay);
      }
    }

    if (mStartTime == 0) {
      mStartTime = SystemClock.uptimeMillis();
    }
    mAnimator.setTarget(targetView);
    mAnimator.start();

    // If this transition was already running, fast forward to where we left.
    if (mPlayedTime > 0) {
      mAnimator.setCurrentPlayTime(mPlayedTime);
    }
  }

  void stop() {
    if (!mAnimator.isRunning()) {
      throw new IllegalStateException("stop() called but the Animator wasn't running.");
    }

    mPlayedTime = mAnimator.getCurrentPlayTime();
    // Don't trigger end Listeners when the animation is manually interrupted.
    mAnimator.removeAllListeners();
    mAnimator.removeAllUpdateListeners();
    mAnimator.end();
    mAnimator.setTarget(null);
  }
}
