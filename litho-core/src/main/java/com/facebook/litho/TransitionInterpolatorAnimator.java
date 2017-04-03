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
  }

  @Override
  public void start(View targetView, List<PropertyChangeHolder> propertyChangeHolders) {
    if (mListener == null) {
      throw new IllegalStateException("TransitionInterpolatorAnimator should have a listener " +
          "set before start() is called in order to callback the TransitionManager at the " +
          "end of the animation.");
    }

    mAnimator.setValues(createPropertyValuesHoldersFrom(propertyChangeHolders));

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

  @Override
  public TransitionInterpolatorAnimator stop() {
    if (!mAnimator.isRunning()) {
      throw new IllegalStateException("stop() called but the Animator wasn't running.");
    }

    mPlayedTime = mAnimator.getCurrentPlayTime();
    // Don't trigger end Listeners when the animation is manually interrupted.
    mAnimator.removeAllListeners();
    mAnimator.removeAllUpdateListeners();
    mAnimator.end();
    mAnimator.setTarget(null);

    return this;
  }

  @Override
  public TransitionAnimator clone() {
    final TransitionInterpolatorAnimator a = new TransitionInterpolatorAnimator();
    a.mSetFlags = mSetFlags;
    a.mDuration = mDuration;
    a.mStartDelay = mStartDelay;
    a.mInterpolator = mInterpolator;
    a.mStartTime = mStartTime;
    a.mPlayedTime = mPlayedTime;
    a.mListener = mListener;

    return a;
  }

  private static PropertyValuesHolder[] createPropertyValuesHoldersFrom(
      List<PropertyChangeHolder> propertyChangeHolders) {
    final int propertyChangesSize = propertyChangeHolders.size();
    final PropertyValuesHolder[] propertyValuesHolders =
        new PropertyValuesHolder[propertyChangesSize];

    for (int i = 0; i < propertyChangesSize; i++) {
      PropertyChangeHolder changeHolder = propertyChangeHolders.get(i);
      propertyValuesHolders[i] = PropertyValuesHolder.ofFloat(
          TransitionProperties.getViewPropertyFrom(changeHolder.propertyType),
          changeHolder.start,
          changeHolder.end);
    }

    return propertyValuesHolders;
  }

  public static class Builder {

    final TransitionInterpolatorAnimator mAnimator;

    Builder() {
      mAnimator = new TransitionInterpolatorAnimator();
    }

    /**
     * Interpolator used for this TransitionInterpolatorAnimator.
     * If not set, an {@link AccelerateDecelerateInterpolator} will be used.
     */
    public Builder interpolator(Interpolator interpolator) {
      mAnimator.setInterpolator(interpolator);
      return this;
    }

    /**
     * Duration used for this TransitionInterpolatorAnimator.
     * If not set, the value of 300ms will be used.
     */
    public Builder duration(int duration) {
      mAnimator.setDuration(duration);
      return this;
    }

    /**
     * StartDelay used for this TransitionInterpolatorAnimator.
     */
    public Builder startDelay(int delay) {
      mAnimator.setStartDelay(delay);
      return this;
    }

    public TransitionAnimator build() {
      return mAnimator;
    }
  }
}
