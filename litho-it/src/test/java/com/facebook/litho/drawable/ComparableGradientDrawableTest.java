/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import android.graphics.drawable.GradientDrawable;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class ComparableGradientDrawableTest {

  @Test
  public void ComparableDrawable_create() {
    ComparableGradientDrawable comparable = new ComparableGradientDrawable();

    assertThat(comparable).isNotNull();
  }

  @Test
  public void ComparableDrawable_equal_simple() {
    ComparableGradientDrawable comparable1 = new ComparableGradientDrawable();
    ComparableGradientDrawable comparable2 = new ComparableGradientDrawable();

    assertThat(comparable1.isEquivalentTo(comparable1)).isTrue();
    assertThat(comparable1.isEquivalentTo(comparable2)).isTrue();
  }

  @Test
  public void ComparableDrawable_equal_detailed() {
    ComparableGradientDrawable comparable1 = new ComparableGradientDrawable();
    comparable1.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
    comparable1.setColors(new int[] {0, 0});
    comparable1.setCornerRadii(new float[] {0, 1, 2, 4});
    comparable1.setSize(1, 2);
    comparable1.setShape(GradientDrawable.OVAL);
    comparable1.setGradientRadius(2);
    comparable1.setGradientType(GradientDrawable.LINEAR_GRADIENT);

    ComparableGradientDrawable comparable2 = new ComparableGradientDrawable();
    comparable2.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
    comparable2.setColors(new int[] {0, 0});
    comparable2.setCornerRadii(new float[] {0, 1, 2, 4});
    comparable2.setSize(1, 2);
    comparable2.setShape(GradientDrawable.OVAL);
    comparable2.setGradientRadius(2);
    comparable2.setGradientType(GradientDrawable.LINEAR_GRADIENT);

    assertThat(comparable1.isEquivalentTo(comparable1)).isTrue();
    assertThat(comparable1.isEquivalentTo(comparable2)).isTrue();
  }

  @Test
  public void ComparableDrawable_equal_detailed_with_null() {
    ComparableGradientDrawable comparable1 = new ComparableGradientDrawable();
    comparable1.setOrientation(null);
    comparable1.setColors(null);
    comparable1.setColor(0xFF);
    comparable1.setCornerRadii(null);

    ComparableGradientDrawable comparable2 = new ComparableGradientDrawable();
    comparable2.setOrientation(null);
    comparable2.setColors(null);
    comparable2.setColor(0xFF);
    comparable2.setCornerRadii(null);

    assertThat(comparable1.isEquivalentTo(comparable1)).isTrue();
    assertThat(comparable1.isEquivalentTo(comparable2)).isTrue();
  }

  @Test
  public void ComparableDrawable_not_equal_simple() {
    ComparableGradientDrawable comparable1 = new ComparableGradientDrawable();
    ComparableDrawable comparable2 = ComparableColorDrawable.create(Color.BLACK);

    assertThat(comparable1.isEquivalentTo(comparable2)).isFalse();
  }

  @Test
  public void ComparableDrawable_not_equal_detailed() {
    ComparableGradientDrawable comparable1 = new ComparableGradientDrawable();
    comparable1.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
    comparable1.setColors(new int[] {0, 0});
    comparable1.setCornerRadii(new float[] {0, 1, 2, 4});
    comparable1.setSize(1, 2);
    comparable1.setShape(GradientDrawable.OVAL);
    comparable1.setGradientRadius(2);
    comparable1.setGradientType(GradientDrawable.LINEAR_GRADIENT);

    ComparableGradientDrawable comparable2 = new ComparableGradientDrawable();
    comparable2.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
    comparable2.setColors(new int[] {1, 1});
    comparable2.setCornerRadii(new float[] {4, 3, 2, 1});
    comparable2.setSize(2, 1);
    comparable2.setShape(GradientDrawable.RECTANGLE);
    comparable2.setGradientRadius(1);
    comparable2.setGradientType(GradientDrawable.RADIAL_GRADIENT);

    assertThat(comparable1.isEquivalentTo(comparable2)).isFalse();
  }

  @Test
  public void ComparableDrawable_not_equal_detailed_with_null() {
    ComparableGradientDrawable comparable1 = new ComparableGradientDrawable();
    comparable1.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
    comparable1.setColors(new int[] {0, 0});
    comparable1.setCornerRadii(new float[] {0, 1, 2, 4});

    ComparableGradientDrawable comparable2 = new ComparableGradientDrawable();
    comparable2.setOrientation(null);
    comparable2.setColors(null);
    comparable2.setCornerRadii(null);

    assertThat(comparable1.isEquivalentTo(comparable2)).isFalse();
  }
}
