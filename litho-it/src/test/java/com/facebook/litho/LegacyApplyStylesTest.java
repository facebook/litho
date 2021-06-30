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

import android.graphics.Color;
import android.view.View;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.testing.ViewAssertions;
import com.facebook.rendercore.testing.match.MatchNode;
import com.facebook.rendercore.testing.match.ViewMatchNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LegacyApplyStylesTest {

  public @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldDisableDrawableOutputs(false);
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

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldDisableDrawableOutputs();
  }
}
