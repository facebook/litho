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

import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for mainAxisGap and crossAxisGap parameter mapping to YogaGutter values in Row and
 * Column components.
 *
 * These tests verify that:
 * - Row: mainAxisGap maps to YogaGutter.COLUMN (horizontal), crossAxisGap maps to YogaGutter.ROW
 *   (vertical)
 * - Column: mainAxisGap maps to YogaGutter.ROW (vertical), crossAxisGap maps to YogaGutter.COLUMN
 *   (horizontal)
 * - gap parameter maps to YogaGutter.ALL for backward compatibility
 * - Direction-specific gaps take precedence over uniform gap
 */
@RunWith(LithoTestRunner::class)
class FlexboxGapMappingTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  // ===== Row Tests =====

  @Test
  fun row_withMainAxisGap_mapsToColumnGutter() {
    val testLithoView = lithoTestRule.render { Row(mainAxisGap = 16.dp) }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // For Row: mainAxis is horizontal, which maps to YogaGutter.COLUMN
    assertThat(node.columnGapPx).isEqualTo(16)
    assertThat(node.rowGapPx).isEqualTo(0)
    assertThat(node.allGapPx).isEqualTo(0)
  }

  @Test
  fun row_withCrossAxisGap_mapsToRowGutter() {
    val testLithoView = lithoTestRule.render { Row(crossAxisGap = 8.dp) }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // For Row: crossAxis is vertical, which maps to YogaGutter.ROW
    assertThat(node.rowGapPx).isEqualTo(8)
    assertThat(node.columnGapPx).isEqualTo(0)
    assertThat(node.allGapPx).isEqualTo(0)
  }

  @Test
  fun row_withBothMainAndCrossAxisGaps_setsBothGutters() {
    val testLithoView = lithoTestRule.render { Row(mainAxisGap = 16.dp, crossAxisGap = 8.dp) }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // For Row: mainAxis=horizontal (COLUMN), crossAxis=vertical (ROW)
    assertThat(node.columnGapPx).isEqualTo(16)
    assertThat(node.rowGapPx).isEqualTo(8)
    assertThat(node.allGapPx).isEqualTo(0)
  }

  @Test
  fun row_withUniformGap_mapsToAllGutter() {
    val testLithoView = lithoTestRule.render { Row(gap = 12.dp) }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // Backward compatibility: gap parameter maps to YogaGutter.ALL
    assertThat(node.allGapPx).isEqualTo(12)
    assertThat(node.columnGapPx).isEqualTo(0)
    assertThat(node.rowGapPx).isEqualTo(0)
  }

  @Test
  fun row_withDirectionSpecificGaps_takesPrecedenceOverUniformGap() {
    val testLithoView = lithoTestRule.render {
      Row(gap = 10.dp, mainAxisGap = 20.dp, crossAxisGap = 5.dp)
    }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // Direction-specific gaps should be set, not uniform gap
    assertThat(node.columnGapPx).isEqualTo(20)
    assertThat(node.rowGapPx).isEqualTo(5)
    // YogaGutter.ALL should NOT be set when direction-specific gaps are present
    assertThat(node.allGapPx).isEqualTo(0)
  }

  @Test
  fun row_withNullGaps_doesNotSetGutterValues() {
    val testLithoView = lithoTestRule.render { Row() }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // No gaps set, should all be 0 (default)
    assertThat(node.allGapPx).isEqualTo(0)
    assertThat(node.rowGapPx).isEqualTo(0)
    assertThat(node.columnGapPx).isEqualTo(0)
  }

  // ===== Column Tests =====

  @Test
  fun column_withMainAxisGap_mapsToRowGutter() {
    val testLithoView = lithoTestRule.render { Column(mainAxisGap = 16.dp) }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // For Column: mainAxis is vertical, which maps to YogaGutter.ROW
    assertThat(node.rowGapPx).isEqualTo(16)
    assertThat(node.columnGapPx).isEqualTo(0)
    assertThat(node.allGapPx).isEqualTo(0)
  }

  @Test
  fun column_withCrossAxisGap_mapsToColumnGutter() {
    val testLithoView = lithoTestRule.render { Column(crossAxisGap = 8.dp) }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // For Column: crossAxis is horizontal, which maps to YogaGutter.COLUMN
    assertThat(node.columnGapPx).isEqualTo(8)
    assertThat(node.rowGapPx).isEqualTo(0)
    assertThat(node.allGapPx).isEqualTo(0)
  }

  @Test
  fun column_withBothMainAndCrossAxisGaps_setsBothGutters() {
    val testLithoView = lithoTestRule.render { Column(mainAxisGap = 16.dp, crossAxisGap = 8.dp) }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // For Column: mainAxis=vertical (ROW), crossAxis=horizontal (COLUMN)
    assertThat(node.rowGapPx).isEqualTo(16)
    assertThat(node.columnGapPx).isEqualTo(8)
    assertThat(node.allGapPx).isEqualTo(0)
  }

  @Test
  fun column_withUniformGap_mapsToAllGutter() {
    val testLithoView = lithoTestRule.render { Column(gap = 12.dp) }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // Backward compatibility: gap parameter maps to YogaGutter.ALL
    assertThat(node.allGapPx).isEqualTo(12)
    assertThat(node.columnGapPx).isEqualTo(0)
    assertThat(node.rowGapPx).isEqualTo(0)
  }

  @Test
  fun column_withDirectionSpecificGaps_takesPrecedenceOverUniformGap() {
    val testLithoView = lithoTestRule.render {
      Column(gap = 10.dp, mainAxisGap = 20.dp, crossAxisGap = 5.dp)
    }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // Direction-specific gaps should be set, not uniform gap
    assertThat(node.rowGapPx).isEqualTo(20)
    assertThat(node.columnGapPx).isEqualTo(5)
    // YogaGutter.ALL should NOT be set when direction-specific gaps are present
    assertThat(node.allGapPx).isEqualTo(0)
  }

  @Test
  fun column_withNullGaps_doesNotSetGutterValues() {
    val testLithoView = lithoTestRule.render { Column() }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    // No gaps set, should all be 0 (default)
    assertThat(node.allGapPx).isEqualTo(0)
    assertThat(node.rowGapPx).isEqualTo(0)
    assertThat(node.columnGapPx).isEqualTo(0)
  }

  // ===== Cross-Verification Tests (Row vs Column axis mapping) =====

  @Test
  fun rowAndColumn_withSameMainAxisGap_mapToOppositeGutters() {
    val rowView = lithoTestRule.render { Row(mainAxisGap = 16.dp) }
    val columnView = lithoTestRule.render { Column(mainAxisGap = 16.dp) }

    val rowNode = requireNotNull(rowView.currentRootNode?.node)
    val columnNode = requireNotNull(columnView.currentRootNode?.node)

    // Row mainAxis=horizontal (COLUMN), Column mainAxis=vertical (ROW)
    assertThat(rowNode.columnGapPx).isEqualTo(16)
    assertThat(columnNode.rowGapPx).isEqualTo(16)
  }

  @Test
  fun rowAndColumn_withSameCrossAxisGap_mapToOppositeGutters() {
    val rowView = lithoTestRule.render { Row(crossAxisGap = 8.dp) }
    val columnView = lithoTestRule.render { Column(crossAxisGap = 8.dp) }

    val rowNode = requireNotNull(rowView.currentRootNode?.node)
    val columnNode = requireNotNull(columnView.currentRootNode?.node)

    // Row crossAxis=vertical (ROW), Column crossAxis=horizontal (COLUMN)
    assertThat(rowNode.rowGapPx).isEqualTo(8)
    assertThat(columnNode.columnGapPx).isEqualTo(8)
  }

  // ===== DSL Function Tests with Children =====

  @Test
  fun row_dslWithMainAxisGap_correctlyMapsGutterWithChildren() {
    val testLithoView = lithoTestRule.render {
      Row(mainAxisGap = 16.dp) { child(Row()) } // Use Row as a simple child component
    }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    assertThat(node.columnGapPx).isEqualTo(16)
    assertThat(node.childCount).isEqualTo(1)
  }

  @Test
  fun column_dslWithCrossAxisGap_correctlyMapsGutterWithChildren() {
    val testLithoView = lithoTestRule.render {
      Column(crossAxisGap = 8.dp) { child(Column()) } // Use Column as a simple child component
    }
    val node = requireNotNull(testLithoView.currentRootNode?.node)

    assertThat(node.columnGapPx).isEqualTo(8)
    assertThat(node.childCount).isEqualTo(1)
  }
}
