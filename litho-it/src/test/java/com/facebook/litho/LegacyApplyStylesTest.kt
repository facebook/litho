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
package com.facebook.litho

import android.graphics.Color
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.litho.it.R
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.testing.ViewAssertions
import com.facebook.rendercore.testing.match.MatchNode
import com.facebook.rendercore.testing.match.ViewMatchNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LegacyApplyStylesTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false)
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  @Test
  fun styles_withAccessibilityAndContentDescriptionStyle_appliesAccessibilityAndContentDescription() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(legacyLithoViewRule.context)
                .wrapInView()
                .child(
                    Column.create(
                            legacyLithoViewRule.context,
                            0,
                            R.style.ApplyStylesTest_AccessibilityAndContentDescription)
                        .flexGrow(1f)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow()
    ViewAssertions.assertThat(legacyLithoViewRule.lithoView)
        .matches(
            ViewMatchNode.forType(LithoView::class.java)
                .child(
                    ViewMatchNode.forType(ComponentHost::class.java)
                        .prop("contentDescription", "Test Content Description")
                        .prop("isImportantForAccessibility", true)))
  }

  @Test
  fun styles_withDuplicateParentStateStyle_appliesDuplicateParentState() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(legacyLithoViewRule.context)
                .wrapInView()
                .child(
                    Column.create(
                            legacyLithoViewRule.context,
                            0,
                            R.style.ApplyStylesTest_DuplicateParentState)
                        .flexGrow(1f)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow()
    ViewAssertions.assertThat(legacyLithoViewRule.lithoView)
        .matches(
            ViewMatchNode.forType(LithoView::class.java)
                .child(
                    ViewMatchNode.forType(ComponentHost::class.java)
                        .prop("isDuplicateParentStateEnabled", true)))
  }

  @Test
  fun styles_withBackgroundForegroundStyle_appliesBackgroundAndForeground() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(legacyLithoViewRule.context)
                .wrapInView()
                .child(
                    Column.create(
                            legacyLithoViewRule.context,
                            0,
                            R.style.ApplyStylesTest_BackgroundForeground)
                        .flexGrow(1f)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow()
    ViewAssertions.assertThat(legacyLithoViewRule.lithoView)
        .matches(
            ViewMatchNode.forType(LithoView::class.java)
                .child(
                    ViewMatchNode.forType(ComponentHost::class.java)
                        .prop(
                            "drawables",
                            MatchNode.list(
                                MatchNode.forType(MatrixDrawable::class.java)
                                    .prop(
                                        "mountedDrawable",
                                        MatchNode.forType(ComparableColorDrawable::class.java)
                                            .prop("color", Color.WHITE)),
                                MatchNode.forType(MatrixDrawable::class.java)
                                    .prop(
                                        "mountedDrawable",
                                        MatchNode.forType(ComparableColorDrawable::class.java)
                                            .prop("color", Color.BLACK))))))
  }
}
