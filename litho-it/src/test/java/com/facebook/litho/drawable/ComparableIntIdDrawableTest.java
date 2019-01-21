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
package com.facebook.litho.drawable;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import com.facebook.litho.testing.drawable.TestColorDrawable;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class ComparableIntIdDrawableTest {

  @Test
  public void ComparableDrawable_create() {
    Drawable drawable = new TestColorDrawable(Color.BLACK);
    ComparableIntIdDrawable comparable = ComparableIntIdDrawable.create(drawable, 0);

    assertThat(comparable.getWrappedDrawable()).isNotNull();
    assertThat(comparable.getWrappedDrawable()).isSameAs(drawable);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ComparableDrawable_create_null() {
    ComparableIntIdDrawable.create(null, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ComparableDrawable_create_comparable() {
    ComparableIntIdDrawable.create(ComparableColorDrawable.create(Color.BLACK), 0);
  }

  @Test
  public void ComparableDrawable_isEquivalentTo_true_if_id_equal() {
    Drawable d1 = new TestColorDrawable(Color.BLACK);
    Drawable d2 = new TestColorDrawable(Color.BLUE);

    ComparableIntIdDrawable comparable1 = ComparableIntIdDrawable.create(d1, 0);
    ComparableIntIdDrawable comparable2 = ComparableIntIdDrawable.create(d2, 0);

    assertThat(comparable1.isEquivalentTo(comparable1)).isTrue();
    assertThat(comparable2.isEquivalentTo(comparable2)).isTrue();

    assertThat(comparable1.isEquivalentTo(comparable2)).isTrue();
  }

  @Test
  public void ComparableDrawable_isEquivalentTo_false_if_id_not_equal() {
    Drawable d1 = new TestColorDrawable(Color.BLACK);

    ComparableIntIdDrawable comparable1 = ComparableIntIdDrawable.create(d1, 0);
    ComparableIntIdDrawable comparable2 = ComparableIntIdDrawable.create(d1, 1);

    assertThat(comparable1.isEquivalentTo(comparable2)).isFalse();
  }

  @Test
  public void ComparableDrawable_isEquivalentTo_null() {
    Drawable drawable = new TestColorDrawable(Color.BLACK);
    ComparableIntIdDrawable comparable1 = ComparableIntIdDrawable.create(drawable, 0);

    assertThat(comparable1.isEquivalentTo(null)).isFalse();
  }
}
