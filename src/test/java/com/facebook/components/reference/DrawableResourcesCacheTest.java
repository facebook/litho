/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(ComponentsTestRunner.class)
public class DrawableResourcesCacheTest {

  private DrawableResourcesCache mCache;

  @Before
  public void setup() {
    mCache = new DrawableResourcesCache();
  }

  @Test
  public void testPoolIsNeverEmpty() {

    Resources resources = RuntimeEnvironment.application.getResources();
    // This being null is ok since we are only using this drawable to test the cache.
    // We still need to declare the variable though otherewise the call to the constructor would be
    // ambiguous.
    Bitmap bitmap = null;
    BitmapDrawable drawable = new BitmapDrawable(resources, bitmap);

    mCache.release(drawable, 1);
    mCache.release(new ColorDrawable(), 2);

    Drawable first = mCache.get(1, resources);
    Drawable second = mCache.get(1, resources);
    Drawable third = mCache.get(2, resources);

    assertNotNull(first);
    assertNotNull(second);
    assertNotNull(third);

    assertEquals(first.getConstantState(), second.getConstantState());
    assertNotEquals(first.getConstantState(), third.getConstantState());
  }

  @Test
  public void testReleaseAndGet() {
    Resources resources = RuntimeEnvironment.application.getResources();
