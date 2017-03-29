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

import android.view.View;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.TransitionProperties.PropertyChangeHolder;
import com.facebook.litho.TransitionProperties.PropertySetHolder;
import com.facebook.litho.TransitionProperties.PropertyType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class TransitionPropertiesTest {

  private static final float TRANSLATION_X = 20f;
  private static final float TRANSLATION_Y = 35f;
  private static final float ALPHA = 0.1f;

  private PropertySetHolder mPropertySetHolder;
  private View mView;

  @Before
  public void setup() {
    mPropertySetHolder = new PropertySetHolder();
    mView = new View(RuntimeEnvironment.application);
  }

  @Test
  public void testSetOneValueAndValuesFlag() {
    assertEquals(PropertyType.NONE, mPropertySetHolder.getPropertyFlags());

    mPropertySetHolder.set(PropertyType.ALPHA, ALPHA);

    final @PropertyType int expectedFlag = PropertyType.ALPHA;
    assertEquals(expectedFlag, mPropertySetHolder.getPropertyFlags());
    assertTrue(mPropertySetHolder.has(PropertyType.ALPHA));
    assertFalse(mPropertySetHolder.has(PropertyType.TRANSLATION_X));
    assertFalse(mPropertySetHolder.has(PropertyType.TRANSLATION_Y));
  }

  @Test
  public void testSetAllValuesFlag() {
    assertEquals(PropertyType.NONE, mPropertySetHolder.getPropertyFlags());

    setAllValues(mPropertySetHolder);

    final @PropertyType int expectedFlag = PropertyType.ALPHA
        | PropertyType.TRANSLATION_X
        | PropertyType.TRANSLATION_Y;
    assertEquals(expectedFlag, mPropertySetHolder.getPropertyFlags());
    assertTrue(mPropertySetHolder.has(PropertyType.ALPHA));
    assertTrue(mPropertySetHolder.has(PropertyType.TRANSLATION_X));
    assertTrue(mPropertySetHolder.has(PropertyType.TRANSLATION_Y));
  }

  @Test
  public void testSetAndApplyValues() {
    setAllValues(mPropertySetHolder);
    mPropertySetHolder.applyProperties(mView);

    assertEquals(ALPHA, mView.getAlpha());
    assertEquals(TRANSLATION_X, mView.getTranslationX());
    assertEquals(TRANSLATION_Y, mView.getTranslationY());
  }

  @Test
  public void testAddValuesWithDifferentValues() {
    final PropertySetHolder vh = new PropertySetHolder();
    vh.set(PropertyType.TRANSLATION_X, TRANSLATION_X);
    mPropertySetHolder.set(PropertyType.ALPHA, ALPHA);

    assertFalse(mPropertySetHolder.has(PropertyType.TRANSLATION_X));

    mPropertySetHolder.addProperties(vh);

    assertTrue(mPropertySetHolder.has(PropertyType.ALPHA));
    assertTrue(mPropertySetHolder.has(PropertyType.TRANSLATION_X));
    assertFalse(mPropertySetHolder.has(PropertyType.TRANSLATION_Y));

    assertFalse(vh.has(PropertyType.ALPHA));
    assertTrue(vh.has(PropertyType.TRANSLATION_X));

    mPropertySetHolder.applyProperties(mView);
    assertEquals(ALPHA, mView.getAlpha());
    assertEquals(TRANSLATION_X, mView.getTranslationX());
    assertEquals(0f, mView.getTranslationY());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddValuesWithSameValuesThrowException() {
    final PropertySetHolder vh = new PropertySetHolder();
    vh.set(PropertyType.TRANSLATION_X, TRANSLATION_X);
    vh.set(PropertyType.ALPHA, ALPHA);
    mPropertySetHolder.set(PropertyType.ALPHA, ALPHA);

    assertTrue(mPropertySetHolder.has(PropertyType.ALPHA));
    assertTrue(vh.has(PropertyType.ALPHA));

    mPropertySetHolder.addProperties(vh);
  }

  @Test
  public void testRecordValuesWithOneValueFlag() {
    mView.setAlpha(ALPHA);
    mView.setTranslationX(TRANSLATION_X);

    mPropertySetHolder.recordProperties(PropertyType.ALPHA, mView);

    final View tmpView = new View(RuntimeEnvironment.application);
    mPropertySetHolder.applyProperties(tmpView);

    assertTrue(mPropertySetHolder.has(PropertyType.ALPHA));
    assertFalse(mPropertySetHolder.has(PropertyType.TRANSLATION_X));
    assertEquals(ALPHA, tmpView.getAlpha());
    assertEquals(0f, tmpView.getTranslationX());
  }

  @Test
  public void testRecordValuesWithMultipleValuesFlag() {
    mView.setAlpha(ALPHA);
    mView.setTranslationX(TRANSLATION_X);

    mPropertySetHolder.recordProperties(
        PropertyType.ALPHA | PropertyType.TRANSLATION_X, mView);

    final View tmpView = new View(RuntimeEnvironment.application);
    mPropertySetHolder.applyProperties(tmpView);

    assertTrue(mPropertySetHolder.has(PropertyType.ALPHA));
    assertTrue(mPropertySetHolder.has(PropertyType.TRANSLATION_X));
    assertEquals(ALPHA, tmpView.getAlpha());
    assertEquals(TRANSLATION_X, tmpView.getTranslationX());
  }

  @Test
  public void testCreatePropertyChangeHoldersWithGoodInput() {
    final PropertySetHolder startHolder = new PropertySetHolder();
    final PropertySetHolder endHolder = new PropertySetHolder();

    startHolder.set(PropertyType.ALPHA, 0f);
    startHolder.set(PropertyType.TRANSLATION_X, TRANSLATION_X);
    endHolder.set(PropertyType.ALPHA, ALPHA);

    final List<PropertyChangeHolder> emptyPropertiesHolder =
        TransitionProperties.createPropertyChangeHolderList(
            startHolder,
            endHolder,
            PropertyType.TRANSLATION_Y);
    final List<PropertyChangeHolder> propertiesHolderAlpha =
        TransitionProperties.createPropertyChangeHolderList(
            startHolder,
            endHolder,
            PropertyType.ALPHA);

    assertEquals(0, emptyPropertiesHolder.size());
    assertEquals(1, propertiesHolderAlpha.size());
    assertEquals(PropertyType.ALPHA, propertiesHolderAlpha.get(0).propertyType);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreatePropertyChangeHoldersWithBadInput() {
    final PropertySetHolder startHolder = new PropertySetHolder();
    final PropertySetHolder endHolder = new PropertySetHolder();

    startHolder.set(PropertyType.ALPHA, 0f);
    endHolder.set(PropertyType.TRANSLATION_Y, TRANSLATION_Y);

    TransitionProperties.createPropertyChangeHolderList(
        startHolder,
        endHolder,
        PropertyType.ALPHA);
  }

  private static void setAllValues(PropertySetHolder propertySetHolder) {
    propertySetHolder.set(PropertyType.ALPHA, ALPHA);
    propertySetHolder.set(PropertyType.TRANSLATION_X, TRANSLATION_X);
    propertySetHolder.set(PropertyType.TRANSLATION_Y, TRANSLATION_Y);
  }
}
