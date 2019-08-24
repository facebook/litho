/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
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

/** Tests {@link TextureWarmer}. */
@RunWith(ComponentsTestRunner.class)
@Config(shadows = TextureWarmerTest.ShadowPicture.class)
public class TextureWarmerTest {
  private ShadowLooper mShadowLooper;
  private TextureWarmer mTextureWarmer;

  @Before
  public void setup() {
    mTextureWarmer = TextureWarmer.getInstance();
    mShadowLooper = Shadows.shadowOf(mTextureWarmer.getWarmerLooper());
  }

  @Test
  public void testWarmGlyph() {
    Layout layout = mock(Layout.class);
    mTextureWarmer.warmLayout(layout);
    mShadowLooper.runOneTask();
    verify(layout).draw(any(Canvas.class));
  }

  @Test
  public void testWarmTexture() {
    Drawable drawable = mock(Drawable.class);
    TextureWarmer.WarmDrawable warmDrawable = new TextureWarmer.WarmDrawable(drawable, 1, 1);
    mTextureWarmer.warmDrawable(warmDrawable);
    mShadowLooper.runOneTask();
    verify(drawable).draw(any(Canvas.class));
  }

  @Implements(Picture.class)
  public static class ShadowPicture {

    @Implementation
    public void __constructor__(int nativePicture, boolean fromStream) {}

    @Implementation
    public void __constructor__(int nativePicture) {}

    @Implementation
    public void __constructor__() {}

    @Implementation
    public Canvas beginRecording(int width, int height) {
      return new Canvas();
    }
  }
}
