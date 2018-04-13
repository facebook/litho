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

package com.facebook.litho.reference;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DrawableResourcesCacheTest {

  private DrawableResourcesCache mCache;

  @Before
  public void setup() {
    mCache = new DrawableResourcesCache();
  }

  @Test
  public void testPoolIsNeverEmpty() {

    Resources resources = application.getResources();
    // This being null is ok since we are only using this drawable to test the cache.
    // We still need to declare the variable though otherewise the call to the constructor would be
    // ambiguous.
    Bitmap bitmap = null;
    BitmapDrawable drawable = new BitmapDrawable(resources, bitmap);

    mCache.release(drawable, 1);
    mCache.release(new ColorDrawable(), 2);

    Drawable first = mCache.get(1, resources, null);
    Drawable second = mCache.get(1, resources, null);
    Drawable third = mCache.get(2, resources, null);

    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(third).isNotNull();

    assertThat(second.getConstantState()).isEqualTo(first.getConstantState());
    assertNotEquals(first.getConstantState(), third.getConstantState());
  }

  @Test
  public void testReleaseAndGet() {
    Resources resources = application.getResources();

    ColorDrawable drawable = new ColorDrawable();
    ColorDrawable drawable2 = new ColorDrawable();
    ColorDrawable drawable3 = new ColorDrawable();
    mCache.release(drawable, 1);
    mCache.release(drawable2, 1);
    mCache.release(drawable3, 1);

    assertThat(mCache.get(1, resources)).isEqualTo(drawable);
    assertThat(mCache.get(1, resources)).isEqualTo(drawable2);
    assertThat(mCache.get(1, resources)).isEqualTo(drawable3);
  }

}
