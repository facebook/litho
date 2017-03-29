/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.reference;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.components.ComponentContext;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class DrawableReferenceTest {

  @Test
  public void testAcquire() {
    Drawable drawable = new ColorDrawable();
    ComponentContext context = new ComponentContext(RuntimeEnvironment.application);

    assertEquals(
        Reference.acquire(
            context,
            DrawableReference.create()
                .drawable(drawable)
                .build()),
        drawable);
  }

}

