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
import static com.facebook.litho.it.R.attr.testAttrDimen;
import static com.facebook.litho.it.R.attr.testAttrDrawable;
import static com.facebook.litho.it.R.attr.undefinedAttrDimen;
import static com.facebook.litho.it.R.attr.undefinedAttrDrawable;
import static com.facebook.litho.it.R.dimen.default_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen_float;
import static com.facebook.litho.it.R.drawable.test_bg;
import static com.facebook.litho.it.R.style.TestTheme;
import static com.facebook.yoga.YogaEdge.LEFT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class ResolveAttributeTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void setup() {
    mLithoViewRule.useContext(
        new ComponentContext(new ContextThemeWrapper(getApplicationContext(), TestTheme)));
  }

  @Test
  public void testResolveDrawableAttribute() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column = Column.create(c).backgroundAttr(testAttrDrawable, 0).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    Drawable d = c.getResources().getDrawable(test_bg);
    Drawable drawable = mLithoViewRule.getCurrentRootNode().getBackground();
    assertThat(shadowOf(drawable).getCreatedFromResId())
        .isEqualTo(shadowOf(d).getCreatedFromResId());
  }

  @Test
  public void testResolveDimenAttribute() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column = Column.create(c).widthAttr(testAttrDimen, default_dimen).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    final LithoLayoutResult node = mLithoViewRule.getCurrentRootNode();

    int dimen = c.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertThat((int) node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testDefaultDrawableAttribute() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column = Column.create(c).backgroundAttr(undefinedAttrDrawable, test_bg).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    final LithoLayoutResult node = mLithoViewRule.getCurrentRootNode();

    Drawable d = c.getResources().getDrawable(test_bg);
    Drawable drawable = node.getBackground();
    assertThat(shadowOf(drawable).getCreatedFromResId())
        .isEqualTo(shadowOf(d).getCreatedFromResId());
  }

  @Test
  public void testDefaultDimenAttribute() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column = Column.create(c).widthAttr(undefinedAttrDimen, test_dimen).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    final LithoLayoutResult node = mLithoViewRule.getCurrentRootNode();

    int dimen = c.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertThat((int) node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenWidthAttribute() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column = Column.create(c).widthAttr(undefinedAttrDimen, test_dimen_float).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    final LithoLayoutResult node = mLithoViewRule.getCurrentRootNode();

    int dimen = c.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenPaddingAttribute() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Column column =
        Column.create(c).paddingAttr(LEFT, undefinedAttrDimen, test_dimen_float).build();

    mLithoViewRule
        .setRootAndSizeSpec(column, MeasureSpecUtils.unspecified(), MeasureSpecUtils.unspecified())
        .measure()
        .layout();

    final LithoLayoutResult node = mLithoViewRule.getCurrentRootNode();

    int dimen = c.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getPaddingLeft()).isEqualTo(dimen);
  }
}
