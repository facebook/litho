// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.graphics.Canvas;
import android.graphics.Picture;
import android.text.Layout;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;

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
