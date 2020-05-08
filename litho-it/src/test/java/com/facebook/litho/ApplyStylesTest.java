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

import android.graphics.Color;
import android.view.View;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.testing.ViewAssertions;
import com.facebook.rendercore.testing.match.MatchNode;
import com.facebook.rendercore.testing.match.ViewMatchNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ApplyStylesTest {

  public @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void setup() {
    setComponentStyleableAttributes();
  }

  @Test
  public void styles_withWidthHeightStyle_appliesWidthHeight() {
    setComponentStyleableAttributes();

    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        .setRoot(
            Column.create(mLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_WidthHeight)
                .build())
        .measure()
        .layout()
        .attachToWindow();

    assertThat(mLithoViewRule.getLithoView().getWidth()).isEqualTo(37);
    assertThat(mLithoViewRule.getLithoView().getHeight()).isEqualTo(100);
  }

  @Test
  public void styles_withMinWidthHeightStyle_appliesMinWidthHeight() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        .setRoot(
            Column.create(mLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_MinWidthHeight)
                .build())
        .measure()
        .layout()
        .attachToWindow();

    assertThat(mLithoViewRule.getLithoView().getWidth()).isEqualTo(50);
    assertThat(mLithoViewRule.getLithoView().getHeight()).isEqualTo(75);
  }

  @Test
  public void styles_withPaddingLeftTopRightBottomStyle_appliesPadding() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(
                    mLithoViewRule.getContext(),
                    0,
                    R.style.ApplyStylesTest_PaddingLeftTopRightBottom)
                .child(Column.create(mLithoViewRule.getContext()).flexGrow(1).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(View.class)
                        .bounds(10, 20, 100 - 10 - 30, 100 - 20 - 40)));
  }

  @Test
  public void styles_withPaddingAllStyle_appliesPadding() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_PaddingAll)
                .child(Column.create(mLithoViewRule.getContext()).flexGrow(1).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(View.class)
                        .bounds(15, 15, 100 - 15 - 15, 100 - 15 - 15)));
  }

  @Ignore("T66670905")
  @Test
  public void styles_withPaddingStartEndStyle_appliesPadding() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_PaddingStartEnd)
                .child(Column.create(mLithoViewRule.getContext()).flexGrow(1).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(ViewMatchNode.forType(View.class).bounds(20, 0, 100 - 20 - 40, 100)));
  }

  @Test
  public void styles_withMarginLeftTopRightBottomStyle_appliesMargin() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLithoViewRule.getContext())
                .child(
                    Column.create(
                            mLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_MarginLeftTopRightBottom)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(View.class)
                        .bounds(10, 20, 100 - 10 - 30, 100 - 20 - 40)));
  }

  @Test
  public void styles_withMarginAllStyle_appliesMargin() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLithoViewRule.getContext())
                .child(
                    Column.create(mLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_MarginAll)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(View.class)
                        .bounds(15, 15, 100 - 15 - 15, 100 - 15 - 15)));
  }

  @Ignore("T66670905")
  @Test
  public void styles_withMarginStartEndStyle_appliesMargin() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLithoViewRule.getContext())
                .child(
                    Column.create(
                            mLithoViewRule.getContext(), 0, R.style.ApplyStylesTest_MarginStartEnd)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(ViewMatchNode.forType(View.class).bounds(10, 0, 100 - 10 - 30, 100)));
  }

  @Test
  public void styles_withBackgroundForegroundStyle_appliesBackgroundAndForeground() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLithoViewRule.getContext())
                .wrapInView()
                .child(
                    Column.create(
                            mLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_BackgroundForeground)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(ComponentHost.class)
                        .prop(
                            "drawables",
                            MatchNode.list(
                                MatchNode.forType(MatrixDrawable.class)
                                    .prop(
                                        "mountedDrawable",
                                        MatchNode.forType(ComparableColorDrawable.class)
                                            .prop("color", Color.WHITE)),
                                MatchNode.forType(MatrixDrawable.class)
                                    .prop(
                                        "mountedDrawable",
                                        MatchNode.forType(ComparableColorDrawable.class)
                                            .prop("color", Color.BLACK))))));
  }

  @Test
  public void
      styles_withAccessibilityAndContentDescriptionStyle_appliesAccessibilityAndContentDescription() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLithoViewRule.getContext())
                .wrapInView()
                .child(
                    Column.create(
                            mLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_AccessibilityAndContentDescription)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(ComponentHost.class)
                        .prop("contentDescription", "Test Content Description")
                        .prop("isImportantForAccessibility", true)));
  }

  @Test
  public void styles_withDuplicateParentStateStyle_appliesDuplicateParentState() {
    mLithoViewRule
        .setSizeSpecs(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        .setRoot(
            Row.create(mLithoViewRule.getContext())
                .wrapInView()
                .child(
                    Column.create(
                            mLithoViewRule.getContext(),
                            0,
                            R.style.ApplyStylesTest_DuplicateParentState)
                        .flexGrow(1)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(ComponentHost.class)
                        .prop("isDuplicateParentStateEnabled", true)));
  }

  /**
   * This is a hack around a BUCK and/or Robolectric issue where we use incorrect resource values.
   * Component tests require the correct values for the ComponentLayout styleable array in order to
   * bind props to the layout. The array that we define here needs to be kept up-to-date with the
   * attrs defined in litho-core/src/main/res/*attrs.xml files.
   */
  static void setComponentStyleableAttributes() {

    System.arraycopy(
        new int[] {
          com.facebook.litho.R.attr.flex_direction,
          com.facebook.litho.R.attr.flex_layoutDirection,
          com.facebook.litho.R.attr.flex_justifyContent,
          com.facebook.litho.R.attr.flex_alignItems,
          com.facebook.litho.R.attr.flex_alignSelf,
          com.facebook.litho.R.attr.flex_positionType,
          com.facebook.litho.R.attr.flex_wrap,
          com.facebook.litho.R.attr.flex_left,
          com.facebook.litho.R.attr.flex_top,
          com.facebook.litho.R.attr.flex_right,
          com.facebook.litho.R.attr.flex_bottom,
          com.facebook.litho.R.attr.flex,
          android.R.attr.layout_width,
          android.R.attr.layout_height,
          android.R.attr.padding,
          android.R.attr.paddingLeft,
          android.R.attr.paddingTop,
          android.R.attr.paddingRight,
          android.R.attr.paddingBottom,
          android.R.attr.paddingStart,
          android.R.attr.paddingEnd,
          android.R.attr.layout_margin,
          android.R.attr.layout_marginLeft,
          android.R.attr.layout_marginTop,
          android.R.attr.layout_marginRight,
          android.R.attr.layout_marginBottom,
          android.R.attr.layout_marginStart,
          android.R.attr.layout_marginEnd,
          android.R.attr.contentDescription,
          android.R.attr.background,
          android.R.attr.foreground,
          android.R.attr.minHeight,
          android.R.attr.minWidth,
          android.R.attr.importantForAccessibility,
          android.R.attr.duplicateParentState,
        },
        /* srcPos */ 0,
        com.facebook.litho.R.styleable.ComponentLayout,
        /* destPos */ 0,
        com.facebook.litho.R.styleable.ComponentLayout.length);
  }
}
