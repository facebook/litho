// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho;

import java.util.LinkedList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.TransitionProperties.PropertyChangeHolder;
import com.facebook.litho.TransitionProperties.PropertyType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowSystemClock;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class TransitionInterpolatorAnimatorTest {

  private static final int DURATION = 3000;
  private static final int START_DELAY = 600;
  private Interpolator mInterpolator;
  private Animator.AnimatorListener mListenerAdapter;
  private static final List<PropertyChangeHolder> PROPERTY_CHANGE_HOLDERS = new LinkedList<>();
  static {
    PROPERTY_CHANGE_HOLDERS.add(PropertyChangeHolder.create(PropertyType.ALPHA, 0, 1));
  }

  private ObjectAnimator mAnimator;
  private View mView;

  @Before
  public void setUp() {
    mAnimator = new ObjectAnimator();
    mListenerAdapter = new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
      }
    };
    mAnimator.addListener(mListenerAdapter);
    mView = new View(RuntimeEnvironment.application);
    mInterpolator = new LinearInterpolator();
  }

  @Test(expected = IllegalStateException.class)
  public void testStartWithoutListenerThrowException() {
    final TransitionInterpolatorAnimator
        animator = new TransitionInterpolatorAnimator(mAnimator, 0, 0);
    animator.start(mView, PROPERTY_CHANGE_HOLDERS);
  }

  @Test
  public void testObjectAnimatorSetOnStart() {
    final TransitionInterpolatorAnimator
        transitionAnimator = new TransitionInterpolatorAnimator(mAnimator, 0, 0);

    transitionAnimator.setDuration(DURATION);
    transitionAnimator.setStartDelay(START_DELAY);
