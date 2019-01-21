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

import static com.facebook.litho.reference.Reference.acquire;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.drawable.DefaultComparableDrawable;
import com.facebook.litho.testing.drawable.TestColorDrawable;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DrawableReferenceTest {

  @Test
  public void testAcquire() {
    Drawable drawable = new TestColorDrawable(Color.BLACK);
    Context context = new ComponentContext(application).getAndroidContext();

    DefaultComparableDrawable comparable =
        (DefaultComparableDrawable)
            acquire(context, DrawableReference.create(DefaultComparableDrawable.create(drawable)));

    assertThat(comparable.getWrappedDrawable()).isEqualTo(drawable);
  }
}
