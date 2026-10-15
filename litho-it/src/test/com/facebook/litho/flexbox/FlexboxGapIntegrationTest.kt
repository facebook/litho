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

package com.facebook.litho.flexbox

import com.facebook.litho.LithoView
import com.facebook.litho.Style
import com.facebook.litho.assertMatches
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.match
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import com.facebook.rendercore.utils.MeasureSpecUtils.unspecified
import com.facebook.yoga.YogaWrap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Row and Column with wrap=WRAP using different mainAxisGap and crossAxisGap
 * values.
 *
 * These tests verify real-world scenarios where wrapping containers use different spacing between
 * items (mainAxisGap) and between wrapped lines (crossAxisGap), ensuring that layout measurements
 * correctly account for the gaps.
 */
@Suppress("NoAssertionTest")
@RunWith(LithoTestRunner::class)
class FlexboxGapIntegrationTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  // ===== Row with Wrap Tests =====

  @Test
  fun row_withWrapAndMainAxisGap_measuresCorrectly() {
    // Row with wrap: mainAxisGap creates horizontal spacing between children within a line
    // Container width: 100px, each child: 30px, mainAxisGap: 12px
    // Expected: 3 children fit in first line (30 + 12 + 30 + 12 + 30 = 114px > 100px)
    // Actually: 2 children per line (30 + 12 + 30 = 72px fits)
    // Total width: 100px (constrained)
    // Total height: 60px (2 lines × 30px, no crossAxisGap)
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(wrap = YogaWrap.WRAP, mainAxisGap = 12.px, style = Style.width(100.px)) {
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 60) })
  }

  @Test
  fun row_withWrapAndCrossAxisGap_measuresCorrectly() {
    // Row with wrap: crossAxisGap creates vertical spacing between wrapped lines
    // Container width: 100px, each child: 30px, crossAxisGap: 8px
    // No mainAxisGap, so 3 × 30 = 90px fits in 100px
    // Expected: 3 children in first line, 1 child in second line (3 + 1)
    // Total width: 100px (constrained)
    // Total height: 68px (30px + 8px gap + 30px)
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(wrap = YogaWrap.WRAP, crossAxisGap = 8.px, style = Style.width(100.px)) {
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 68) })
  }

  @Test
  fun row_withWrapAndDifferentMainAndCrossGaps_measuresCorrectly() {
    // Row with wrap: mainAxisGap=12px (horizontal), crossAxisGap=8px (vertical)
    // Container width: 100px, each child: 30px
    // Expected layout:
    // - Line 1: child1 (30px) + gap (12px) + child2 (30px) = 72px
    // - gap (8px)
    // - Line 2: child3 (30px) + gap (12px) + child4 (30px) = 72px
    // - gap (8px)
    // - Line 3: child5 (30px)
    // Total width: 100px (constrained)
    // Total height: 30 + 8 + 30 + 8 + 30 = 106px
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(
              wrap = YogaWrap.WRAP,
              mainAxisGap = 12.px,
              crossAxisGap = 8.px,
              style = Style.width(100.px),
          ) {
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 106) })
  }

  @Test
  fun row_withWrapAndRealisticGapValues_measuresCorrectly() {
    // Real-world scenario: Row with wrap using typical UI spacing
    // mainAxisGap: 12px (spacing between items), crossAxisGap: 8px (spacing between rows)
    // Container width: 200px, each child: 50px
    // Expected: 3 children per line (50 + 12 + 50 + 12 + 50 = 174px fits)
    // 2 lines total (6 children)
    // Total height: 40 + 8 + 40 = 88px
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(
              wrap = YogaWrap.WRAP,
              mainAxisGap = 12.px,
              crossAxisGap = 8.px,
              style = Style.width(200.px),
          ) {
            child(Row(style = Style.width(50.px).height(40.px)))
            child(Row(style = Style.width(50.px).height(40.px)))
            child(Row(style = Style.width(50.px).height(40.px)))
            child(Row(style = Style.width(50.px).height(40.px)))
            child(Row(style = Style.width(50.px).height(40.px)))
            child(Row(style = Style.width(50.px).height(40.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 200, 88) })
  }

  @Test
  fun row_withWrapAndMultipleChildrenSameMainAndCrossGaps_appliesGapsCorrectly() {
    // Verify gaps are consistently applied between all children
    // Row with wrap, mainAxisGap=10px, crossAxisGap=10px (same value for both)
    // Container width: 150px, each child: 40px
    // Expected: 3 children per line (40 + 10 + 40 + 10 + 40 = 140px fits)
    // 3 lines total (7 children: 3, 3, 1)
    // Total height: 30 + 10 + 30 + 10 + 30 = 110px
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(
              wrap = YogaWrap.WRAP,
              mainAxisGap = 10.px,
              crossAxisGap = 10.px,
              style = Style.width(150.px),
          ) {
            child(Row(style = Style.width(40.px).height(30.px)))
            child(Row(style = Style.width(40.px).height(30.px)))
            child(Row(style = Style.width(40.px).height(30.px)))
            child(Row(style = Style.width(40.px).height(30.px)))
            child(Row(style = Style.width(40.px).height(30.px)))
            child(Row(style = Style.width(40.px).height(30.px)))
            child(Row(style = Style.width(40.px).height(30.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 150, 110) })
  }

  // ===== Column with Wrap Tests =====

  @Test
  fun column_withWrapAndMainAxisGap_measuresCorrectly() {
    // Column with wrap: mainAxisGap creates vertical spacing between children within a column
    // Container height: 100px, each child: 30px, mainAxisGap: 10px
    // Expected: 2 children per column (30 + 10 + 30 = 70px fits)
    // Total height: 100px (constrained)
    // Total width: 60px (2 columns × 30px, no crossAxisGap)
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Column(wrap = YogaWrap.WRAP, mainAxisGap = 10.px, style = Style.height(100.px)) {
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 60, 100) })
  }

  @Test
  fun column_withWrapAndCrossAxisGap_measuresCorrectly() {
    // Column with wrap: crossAxisGap creates horizontal spacing between wrapped columns
    // Container height: 100px, each child: 30px, crossAxisGap: 6px
    // No mainAxisGap, so 3 × 30 = 90px fits in 100px
    // Expected: 3 children in first column, 1 child in second column (3 + 1)
    // Total width: 66px (30px + 6px gap + 30px)
    // Total height: 100px (constrained)
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Column(wrap = YogaWrap.WRAP, crossAxisGap = 6.px, style = Style.height(100.px)) {
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 66, 100) })
  }

  @Test
  fun column_withWrapAndDifferentMainAndCrossGaps_measuresCorrectly() {
    // Column with wrap: mainAxisGap=10px (vertical), crossAxisGap=6px (horizontal)
    // Container height: 100px, each child: 30px
    // Expected layout:
    // - Column 1: child1 (30px) + gap (10px) + child2 (30px) = 70px
    // - gap (6px)
    // - Column 2: child3 (30px) + gap (10px) + child4 (30px) = 70px
    // - gap (6px)
    // - Column 3: child5 (30px)
    // Total width: 30 + 6 + 30 + 6 + 30 = 102px
    // Total height: 100px (constrained)
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Column(
              wrap = YogaWrap.WRAP,
              mainAxisGap = 10.px,
              crossAxisGap = 6.px,
              style = Style.height(100.px),
          ) {
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
            child(Row(style = Style.width(30.px).height(30.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 102, 100) })
  }

  @Test
  fun column_withWrapAndRealisticGapValues_measuresCorrectly() {
    // Real-world scenario: Column with wrap using typical UI spacing
    // mainAxisGap: 10px (spacing between items vertically), crossAxisGap: 6px (spacing between
    // columns)
    // Container height: 200px, each child: 50px
    // Expected: 3 children per column (50 + 10 + 50 + 10 + 50 = 170px fits)
    // 2 columns total (6 children)
    // Total width: 40 + 6 + 40 = 86px
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Column(
              wrap = YogaWrap.WRAP,
              mainAxisGap = 10.px,
              crossAxisGap = 6.px,
              style = Style.height(200.px),
          ) {
            child(Row(style = Style.width(40.px).height(50.px)))
            child(Row(style = Style.width(40.px).height(50.px)))
            child(Row(style = Style.width(40.px).height(50.px)))
            child(Row(style = Style.width(40.px).height(50.px)))
            child(Row(style = Style.width(40.px).height(50.px)))
            child(Row(style = Style.width(40.px).height(50.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 86, 200) })
  }

  @Test
  fun column_withWrapAndMultipleChildrenSameMainAndCrossGaps_appliesGapsCorrectly() {
    // Verify gaps are consistently applied between all children
    // Column with wrap, mainAxisGap=8px, crossAxisGap=8px (same value for both)
    // Container height: 150px, each child: 40px
    // Expected: 3 children per column (40 + 8 + 40 + 8 + 40 = 136px fits)
    // 3 columns total (7 children: 3, 3, 1)
    // Total width: 30 + 8 + 30 + 8 + 30 = 106px
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Column(
              wrap = YogaWrap.WRAP,
              mainAxisGap = 8.px,
              crossAxisGap = 8.px,
              style = Style.height(150.px),
          ) {
            child(Row(style = Style.width(30.px).height(40.px)))
            child(Row(style = Style.width(30.px).height(40.px)))
            child(Row(style = Style.width(30.px).height(40.px)))
            child(Row(style = Style.width(30.px).height(40.px)))
            child(Row(style = Style.width(30.px).height(40.px)))
            child(Row(style = Style.width(30.px).height(40.px)))
            child(Row(style = Style.width(30.px).height(40.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 106, 150) })
  }
}
