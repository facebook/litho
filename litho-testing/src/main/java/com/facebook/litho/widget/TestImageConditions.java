/*
 * Copyright 2018-present Facebook, Inc.
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

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import org.assertj.core.api.Condition;
import org.robolectric.Shadows;

/** Helpers to match against {@link TestImageSpec}. */
public class TestImageConditions {

  /**
   * Temporary workaround for the {@link TestImageSpec}'s drawableRes matcher problem with
   * Robolectric.
   *
   * <p>Matcher that succeeds if a {@link InspectableComponent} has an image content that matches
   * the provided drawable resource id.
   *
   * <p>
   *
   * <h2>Example Use</h2>
   *
   * <pre><code>
   * assertThat(c, mComponent)
   *   .has(
   *     deepSubComponentWith(c, TestImageConditions.imageWithDrawable(R.drawable.drawable_id)));
   * </code></pre>
   */
  public static Condition<InspectableComponent> imageWithDrawable(final @DrawableRes int resId) {
    return new Condition<InspectableComponent>() {
      @Override
      public boolean matches(InspectableComponent value) {
        if (value.getComponentClass() != Image.class) {
          return false;
        }

        Image impl = (Image) value.getComponent();
        final Drawable propValueDrawable = impl.drawable;
        return Shadows.shadowOf(((BitmapDrawable) propValueDrawable).getBitmap())
                .getCreatedFromResId()
            == resId;
      }
    };
  }
}
