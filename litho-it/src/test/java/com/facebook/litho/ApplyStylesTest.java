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

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.testing.ViewAssertions;
import com.facebook.rendercore.testing.match.MatchNode;
import com.facebook.rendercore.testing.match.ViewMatchNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ApplyStylesTest {

  public @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
  }

  @Test
  public void styles_withWidthHeightStyle_appliesWidthHeight() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        .setRoot(
            Column.create(mLegacyLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_WidthHeight)
                .build())
        .measure()
        .layout()
        .attachToWindow();

    assertThat(mLegacyLithoViewRule.getLithoView().getWidth()).isEqualTo(37);
    assertThat(mLegacyLithoViewRule.getLithoView().getHeight()).isEqualTo(100);
  }

  @Test
  public void styles_withMinWidthHeightStyle_appliesMinWidthHeight() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        .setRoot(
            Column.create(
                    mLegacyLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_MinWidthHeight)
                .build())
        .measure()
        .layout()
        .attachToWindow();

    assertThat(mLegacyLithoViewRule.getLithoView().getWidth()).isEqualTo(50);
    assertThat(mLegacyLithoViewRule.getLithoView().getHeight()).isEqualTo(75);
  }

  @Test
  public void styles_withPaddingLeftTopRightBottomStyle_appliesPadding() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(
                    mLegacyLithoViewRule.getContext(),
                    0,
                    R.style.ApplyStylesTest_PaddingLeftTopRightBottom)
                .child(Column.create(mLegacyLithoViewRule.getContext()).flexGrow(1).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(View.class)
                        .bounds(10, 20, 100 - 10 - 30, 100 - 20 - 40)));
  }

  @Test
  public void styles_withPaddingAllStyle_appliesPadding() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLegacyLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_PaddingAll)
                .child(Column.create(mLegacyLithoViewRule.getContext()).flexGrow(1).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(View.class)
                        .bounds(15, 15, 100 - 15 - 15, 100 - 15 - 15)));
  }

  @Ignore("T66670905")
  @Test
  public void styles_withPaddingStartEndStyle_appliesPadding() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(
                    mLegacyLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_PaddingStartEnd)
                .child(Column.create(mLegacyLithoViewRule.getContext()).flexGrow(1).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(ViewMatchNode.forType(View.class).bounds(20, 0, 100 - 20 - 40, 100)));
  }

  @Test
  public void styles_withMarginLeftTopRightBottomStyle_appliesMargin() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLegacyLithoViewRule.getContext())
                .child(
                    Column.create(
                            mLegacyLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_MarginLeftTopRightBottom)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(View.class)
                        .bounds(10, 20, 100 - 10 - 30, 100 - 20 - 40)));
  }

  @Test
  public void styles_withMarginAllStyle_appliesMargin() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLegacyLithoViewRule.getContext())
                .child(
                    Column.create(
                            mLegacyLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_MarginAll)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(View.class)
                        .bounds(15, 15, 100 - 15 - 15, 100 - 15 - 15)));
  }

  @Ignore("T66670905")
  @Test
  public void styles_withMarginStartEndStyle_appliesMargin() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLegacyLithoViewRule.getContext())
                .child(
                    Column.create(
                            mLegacyLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_MarginStartEnd)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(ViewMatchNode.forType(View.class).bounds(10, 0, 100 - 10 - 30, 100)));
  }

  @Test
  public void styles_withBackgroundForegroundStyle_appliesBackgroundAndForeground() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLegacyLithoViewRule.getContext())
                .wrapInView()
                .child(
                    Column.create(
                            mLegacyLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_BackgroundForeground)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    final ComponentHost innerHost =
        (ComponentHost) mLegacyLithoViewRule.getLithoView().getChildAt(0);

    ViewAssertions.assertThat(innerHost)
        .matches(
            ViewMatchNode.forType(ComponentHost.class)
                .child(
                    ViewMatchNode.forType(ComponentHost.class)
                        .prop("background", MatchNode.forType(ColorDrawable.class))));
  }

  @Test
  public void
      styles_withAccessibilityAndContentDescriptionStyle_appliesAccessibilityAndContentDescription() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLegacyLithoViewRule.getContext())
                .wrapInView()
                .child(
                    Column.create(
                            mLegacyLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_AccessibilityAndContentDescription)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    final ComponentHost innerHost =
        (ComponentHost) mLegacyLithoViewRule.getLithoView().getChildAt(0);

    ViewAssertions.assertThat(innerHost)
        .matches(
            ViewMatchNode.forType(ComponentHost.class)
                .child(
                    ViewMatchNode.forType(ComponentHost.class)
                        .prop("contentDescription", "Test Content Description")
                        .prop("isImportantForAccessibility", true)));
  }

  @Test
  public void styles_withDuplicateParentStateStyle_appliesDuplicateParentState() {
    mLegacyLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLegacyLithoViewRule.getContext())
                .wrapInView()
                .child(
                    Column.create(
                            mLegacyLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_DuplicateParentState)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    final ComponentHost innerHost =
        (ComponentHost) mLegacyLithoViewRule.getLithoView().getChildAt(0);

    ViewAssertions.assertThat(innerHost)
        .matches(
            ViewMatchNode.forType(ComponentHost.class)
                .child(
                    ViewMatchNode.forType(ComponentHost.class)
                        .prop("isDuplicateParentStateEnabled", true)));
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
