/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(ComponentsTestRunner.class)
public class ColorDrawableReferenceTest {

  private static final int DEFAULT_ALPHA_VALUE = 255;
  private static final int OTHER_ALPHA_VALUE = 128;

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testAcquire() {
    ColorDrawable drawable =  (ColorDrawable) Reference.acquire(
        mContext,
        ColorDrawableReference.create(mContext)
            .color(Color.BLACK)
            .alpha(DEFAULT_ALPHA_VALUE)
            .build());

    assertEquals(drawable.getColor(), Color.BLACK);
    assertEquals(drawable.getAlpha(), DEFAULT_ALPHA_VALUE);
    assertNotNull(drawable);
  }

  @Test
  public void testReleaseAndAcquireDifferentColorAndAlpha() {
    Reference<Drawable> ref1 =
        ColorDrawableReference.create(mContext)
            .color(Color.BLACK)
            .alpha(OTHER_ALPHA_VALUE)
            .build();
    ColorDrawable colorDrawable1 = (ColorDrawable) Reference.acquire(mContext, ref1);
    Reference.release(mContext, colorDrawable1, ref1);

    Reference<Drawable> ref2 =
        ColorDrawableReference.create(mContext)
            .color(Color.RED)
            .alpha(DEFAULT_ALPHA_VALUE)
            .build();
    ColorDrawable colorDrawable2 =
        (ColorDrawable) Reference.acquire(mContext, ref2);
