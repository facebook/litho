/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.Transition.TransitionType;
import com.facebook.litho.TransitionProperties.PropertyType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class TransitionTest {

  private static final String KEY = "key";
  private static final float START_ALPHA = 0.5f;
  private static final float START_TRANSLATION_X = 30f;
  private static final float END_TRANSLATION_X = 50f;

  private TransitionInterpolatorAnimator mAnimator;
  private TransitionInterpolatorAnimator mAppearAnimator;
  private TransitionInterpolatorAnimator mDisappearAnimator;

  @Before
  public void setUp() {
    mAnimator = (TransitionInterpolatorAnimator) TransitionInterpolatorAnimator.create().build();
    mAppearAnimator = (TransitionInterpolatorAnimator) mAnimator.clone();
    mDisappearAnimator = (TransitionInterpolatorAnimator) mAnimator.clone();
  }

  @Test
  public void testTransitionBuilderWithSimpleChangeTransition() {
    final Transition t = new Transition.Builder(KEY, mAnimator)
        .alpha()
        .build();

    assertFalse(t instanceof TransitionSet);
    assertEquals(KEY, t.getKey());
    assertEquals(TransitionType.CHANGE, t.getTransitionType());
    assertNull(t.getLocalValues());
    assertEquals(PropertyType.ALPHA, t.getValuesFlag());
  }

  @Test
  public void testTransitionBuilderWithAppearTransitionOneValue() {
    Transition t = new Transition.Builder(KEY, mAnimator)
        .alpha()
        .withStartValue(START_ALPHA, mAppearAnimator)
        .build();

    assertTrue(t instanceof TransitionSet);
    final TransitionSet ts = (TransitionSet) t;
    assertEquals(2, ts.size());

    // Assert first on the change Transition.
    t = ts.get(0);

    assertFalse(t instanceof TransitionSet);
    assertEquals(KEY, t.getKey());
    assertEquals(TransitionType.CHANGE, t.getTransitionType());
    assertNull(t.getLocalValues());
    assertEquals(TransitionProperties.PropertyType.ALPHA, t.getValuesFlag());

    // Assert now on the appearing Transition.
    t = ts.get(1);

    assertFalse(t instanceof TransitionSet);
    assertEquals(KEY, t.getKey());
    assertEquals(TransitionType.APPEAR, t.getTransitionType());
    assertEquals(PropertyType.ALPHA, t.getValuesFlag());
    assertNotNull(t.getLocalValues());
    assertEquals(TransitionProperties.PropertyType.ALPHA, t.getLocalValues().getPropertyFlags());
  }

  @Test
  public void testTransitionBuilderWithAppearDisappearTransitionsTwoValue() {
    Transition t = new Transition.Builder(KEY, mAnimator)
        .alpha()
        .withStartValue(START_ALPHA, mAppearAnimator)
        .translationX()
        .withStartValue(START_TRANSLATION_X, mAppearAnimator)
        .withEndValue(END_TRANSLATION_X, mDisappearAnimator)
        .build();

    assertTrue(t instanceof TransitionSet);
    final TransitionSet ts = (TransitionSet) t;
    assertEquals(3, ts.size());
    assertEquals(TransitionType.UNDEFINED, ts.getTransitionType());

    // Assert first on the change Transition.
    t = ts.get(0);

    assertFalse(t instanceof TransitionSet);
    assertEquals(KEY, t.getKey());
    assertEquals(TransitionType.CHANGE, t.getTransitionType());
    assertNull(t.getLocalValues());
    assertEquals(
        PropertyType.ALPHA | TransitionProperties.PropertyType.TRANSLATION_X,
        t.getValuesFlag());

    // Assert now on the appearing Transition.
    t = ts.get(1);

    assertFalse(t instanceof TransitionSet);
    assertEquals(KEY, t.getKey());
    assertEquals(TransitionType.APPEAR, t.getTransitionType());
    assertEquals(
        TransitionProperties.PropertyType.ALPHA | TransitionProperties.PropertyType.TRANSLATION_X,
        t.getValuesFlag());
    assertNotNull(t.getLocalValues());
    assertEquals(
        TransitionProperties.PropertyType.ALPHA | TransitionProperties.PropertyType.TRANSLATION_X,
        t.getLocalValues().getPropertyFlags());

    // Assert now on the disappear Transition.
    t = ts.get(2);

    assertFalse(t instanceof TransitionSet);
    assertEquals(KEY, t.getKey());
    assertEquals(TransitionType.DISAPPEAR, t.getTransitionType());
    assertEquals(TransitionProperties.PropertyType.TRANSLATION_X, t.getValuesFlag());
    assertNotNull(t.getLocalValues());
    assertEquals(PropertyType.TRANSLATION_X, t.getLocalValues().getPropertyFlags());
  }
}
