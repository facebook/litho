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

import android.graphics.drawable.ColorDrawable
import android.view.View
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.it.R
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.rendercore.testing.ViewAssertions
import com.facebook.rendercore.testing.match.MatchNode
import com.facebook.rendercore.testing.match.ViewMatchNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ApplyStylesTest {

  @JvmField
  @Rule
  var legacyLithoViewRule: LegacyLithoViewRule =
      LegacyLithoViewRule(
          ComponentsConfiguration.create().shouldAddHostViewForRootComponent(true).build())

  @Test
  fun styles_withWidthHeightStyle_appliesWidthHeight() {
    legacyLithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot(
            Column.create(legacyLithoViewRule.context, 0, R.style.ApplyStylesTest_WidthHeight)
                .build())
        .measure()
        .layout()
        .attachToWindow()
    assertThat(legacyLithoViewRule.lithoView.width).isEqualTo(37)
    assertThat(legacyLithoViewRule.lithoView.height).isEqualTo(100)
  }

  @Test
  fun styles_withMinWidthHeightStyle_appliesMinWidthHeight() {
    legacyLithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot(
            Column.create(legacyLithoViewRule.context, 0, R.style.ApplyStylesTest_MinWidthHeight)
                .build())
        .measure()
        .layout()
        .attachToWindow()
    assertThat(legacyLithoViewRule.lithoView.width).isEqualTo(50)
    assertThat(legacyLithoViewRule.lithoView.height).isEqualTo(75)
  }

  @Test
  fun styles_withPaddingLeftTopRightBottomStyle_appliesPadding() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(
                    legacyLithoViewRule.context,
                    0,
                    R.style.ApplyStylesTest_PaddingLeftTopRightBottom)
                .child(Column.create(legacyLithoViewRule.context).flexGrow(1f).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow()
    ViewAssertions.assertThat(legacyLithoViewRule.lithoView)
        .matches(
            ViewMatchNode.forType(LithoView::class.java)
                .child(
                    ViewMatchNode.forType(View::class.java)
                        .bounds(10, 20, 100 - 10 - 30, 100 - 20 - 40)))
  }

  @Test
  fun styles_withPaddingAllStyle_appliesPadding() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(legacyLithoViewRule.context, 0, R.style.ApplyStylesTest_PaddingAll)
                .child(Column.create(legacyLithoViewRule.context).flexGrow(1f).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow()
    ViewAssertions.assertThat(legacyLithoViewRule.lithoView)
        .matches(
            ViewMatchNode.forType(LithoView::class.java)
                .child(
                    ViewMatchNode.forType(View::class.java)
                        .bounds(15, 15, 100 - 15 - 15, 100 - 15 - 15)))
  }

  @Ignore("T66670905")
  @Test
  fun styles_withPaddingStartEndStyle_appliesPadding() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(legacyLithoViewRule.context, 0, R.style.ApplyStylesTest_PaddingStartEnd)
                .child(Column.create(legacyLithoViewRule.context).flexGrow(1f).wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow()
    ViewAssertions.assertThat(legacyLithoViewRule.lithoView)
        .matches(
            ViewMatchNode.forType(LithoView::class.java)
                .child(ViewMatchNode.forType(View::class.java).bounds(20, 0, 100 - 20 - 40, 100)))
  }

  @Test
  fun styles_withMarginLeftTopRightBottomStyle_appliesMargin() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(legacyLithoViewRule.context)
                .child(
                    Column.create(
                            legacyLithoViewRule.context,
                            0,
                            R.style.ApplyStylesTest_MarginLeftTopRightBottom)
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
                    ViewMatchNode.forType(View::class.java)
                        .bounds(10, 20, 100 - 10 - 30, 100 - 20 - 40)))
  }

  @Test
  fun styles_withMarginAllStyle_appliesMargin() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(legacyLithoViewRule.context)
                .child(
                    Column.create(legacyLithoViewRule.context, 0, R.style.ApplyStylesTest_MarginAll)
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
                    ViewMatchNode.forType(View::class.java)
                        .bounds(15, 15, 100 - 15 - 15, 100 - 15 - 15)))
  }

  @Ignore("T66670905")
  @Test
  fun styles_withMarginStartEndStyle_appliesMargin() {
    legacyLithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot(
            Row.create(legacyLithoViewRule.context)
                .child(
                    Column.create(
                            legacyLithoViewRule.context, 0, R.style.ApplyStylesTest_MarginStartEnd)
                        .flexGrow(1f)
                        .wrapInView())
                .build())
        .measure()
        .layout()
        .attachToWindow()
    ViewAssertions.assertThat(legacyLithoViewRule.lithoView)
        .matches(
            ViewMatchNode.forType(LithoView::class.java)
                .child(ViewMatchNode.forType(View::class.java).bounds(10, 0, 100 - 10 - 30, 100)))
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
    val innerHost = legacyLithoViewRule.lithoView.getChildAt(0) as ComponentHost
    ViewAssertions.assertThat(innerHost)
        .matches(
            ViewMatchNode.forType(ComponentHost::class.java)
                .child(
                    ViewMatchNode.forType(ComponentHost::class.java)
                        .prop<MatchNode>(
                            "background", MatchNode.forType(ColorDrawable::class.java))))
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
    val innerHost = legacyLithoViewRule.lithoView.getChildAt(0) as ComponentHost
    ViewAssertions.assertThat(innerHost)
        .matches(
            ViewMatchNode.forType(ComponentHost::class.java)
                .child(
                    ViewMatchNode.forType(ComponentHost::class.java)
                        .prop<String>("contentDescription", "Test Content Description")
                        .prop<Boolean>("isImportantForAccessibility", true)))
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
    val innerHost = legacyLithoViewRule.lithoView.getChildAt(0) as ComponentHost
    ViewAssertions.assertThat(innerHost)
        .matches(
            ViewMatchNode.forType(ComponentHost::class.java)
                .child(
                    ViewMatchNode.forType(ComponentHost::class.java)
                        .prop<Boolean>("isDuplicateParentStateEnabled", true)))
  }
}
