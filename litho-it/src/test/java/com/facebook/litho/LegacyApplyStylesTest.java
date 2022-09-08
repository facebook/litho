/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static com.facebook.litho.testing.MeasureSpecTestingUtilsKt.exactly;

import android.graphics.Color;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.LegacyLithoViewRule;
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

  public @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false);
  }

  @Test
  public void
      styles_withAccessibilityAndContentDescriptionStyle_appliesAccessibilityAndContentDescription() {
    mLegacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
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

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(ComponentHost.class)
                        .prop("contentDescription", "Test Content Description")
                        .prop("isImportantForAccessibility", true)));
  }

  @Test
  public void styles_withDuplicateParentStateStyle_appliesDuplicateParentState() {
    mLegacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
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

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .child(
                    ViewMatchNode.forType(ComponentHost.class)
                        .prop("isDuplicateParentStateEnabled", true)));
  }

  @Test
  public void styles_withBackgroundForegroundStyle_appliesBackgroundAndForeground() {
    mLegacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
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

    ViewAssertions.assertThat(mLegacyLithoViewRule.getLithoView())
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
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
