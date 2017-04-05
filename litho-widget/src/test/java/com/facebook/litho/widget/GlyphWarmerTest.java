/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.graphics.Canvas;
import android.graphics.Picture;
import android.text.Layout;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link GlyphWarmer}.
 */
@RunWith(ComponentsTestRunner.class)
@Config(shadows = GlyphWarmerTest.ShadowPicture.class)
public class GlyphWarmerTest {
  private ShadowLooper mShadowLooper;
  private GlyphWarmer mGlyphWarmer;

  @Before
  public void setup() {
    mGlyphWarmer = GlyphWarmer.getInstance();
    mShadowLooper = Shadows.shadowOf(mGlyphWarmer.getWarmerLooper());
  }

  @Test
  public void testWarmGlyph() {
    Layout layout = mock(Layout.class);
    mGlyphWarmer.warmLayout(layout);
    mShadowLooper.runOneTask();
    verify(layout).draw(any(Canvas.class));
  }

  @Implements(Picture.class)
  public static class ShadowPicture {

    @Implementation
    public void __constructor__(int nativePicture, boolean fromStream) {

    }

    @Implementation
    public void __constructor__(int nativePicture) {

    }

    @Implementation
    public void __constructor__() {

    }

    @Implementation
    public Canvas beginRecording(int width, int height) {
      return new Canvas();
    }
  }
}
