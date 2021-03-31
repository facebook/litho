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

package com.facebook.litho;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.it.R.dimen.test_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen_float;
import static com.facebook.litho.it.R.style.TestTheme;
import static com.facebook.yoga.YogaEdge.LEFT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.ContextThemeWrapper;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TextInput;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ResolveResTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void setup() {
    mLithoViewRule.useContext(
        new ComponentContext(new ContextThemeWrapper(getApplicationContext(), TestTheme)));
  }

  @Test
  public void testDefaultDimenWidthRes() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column = Column.create(c).widthRes(test_dimen).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    int dimen = c.getResources().getDimensionPixelSize(test_dimen);
    assertThat(mLithoViewRule.getLithoView().getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testDefaultDimenPaddingRes() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column = Column.create(c).paddingRes(LEFT, test_dimen).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    int dimen = c.getResources().getDimensionPixelSize(test_dimen);
    assertThat(mLithoViewRule.getLithoView().getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenWidthRes() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column = Column.create(c).widthRes(test_dimen_float).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    int dimen = c.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(mLithoViewRule.getLithoView().getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenPaddingRes() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Row row =
        Row.create(c).child(TextInput.create(c).paddingRes(LEFT, test_dimen_float)).build();

    mLithoViewRule.attachToWindow().setSizePx(100, 100).setRoot(row).measure().layout();

    int dimen = c.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(mLithoViewRule.getLithoView().getChildAt(0).getPaddingLeft()).isEqualTo(dimen);
  }
}
