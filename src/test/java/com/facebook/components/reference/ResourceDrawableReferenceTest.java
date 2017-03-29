/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.graphics.drawable.Drawable;

import com.facebook.litho.R;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(ComponentsTestRunner.class)
public class ResourceDrawableReferenceTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testAcquire() {
    Drawable drawable = Reference.acquire(
        mContext,
        ResourceDrawableReference.create(mContext)
            .resId(R.drawable.background)
            .build());
    assertNotNull(drawable);
  }

  @Test
  public void testReleaseAndAcquire() {
