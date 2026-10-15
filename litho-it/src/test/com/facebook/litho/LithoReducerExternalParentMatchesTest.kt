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

import android.graphics.Rect
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * Direct unit tests for [LithoReducer.externalParentMatches]. The end-to-end position-shift
 * scenario is covered by `splice_rejects_when_external_parent_shifts_position` in
 * [SubtreeCacheParityTest]; these tests pin the helper's exact match/reject logic so a regression
 * cannot silently relax either coordinate check.
 */
@RunWith(LithoTestRunner::class)
class LithoReducerExternalParentMatchesTest {

  private fun snapshot(absX: Int, absY: Int): SubtreeSnapshot = SubtreeSnapshot(
      mountableOutputStart = 0,
      mountableOutputEnd = 0,
      visibilityOutputStart = 0,
      visibilityOutputEnd = 0,
      testOutputStart = 0,
      testOutputEnd = 0,
      componentScopeStart = 0,
      componentScopeEnd = 0,
      attachableStart = 0,
      attachableEnd = 0,
      rootX = 0,
      rootY = 0,
      childCount = 0,
      childPrevX = IntArray(0),
      childPrevY = IntArray(0),
      childPrevWidth = IntArray(0),
      childPrevHeight = IntArray(0),
      childRangeStart = IntArray(0),
      diffNode = mock<DiffNode>(),
      externalParentAbsoluteX = absX,
      externalParentAbsoluteY = absY,
  )

  // RTN absoluteX/Y is computed at construction as `(parent?.absoluteX ?: 0) + bounds.left` — so
  // with parent=null and bounds=Rect(absX, absY, ...), absoluteX/Y == absX/absY.
  private fun rtn(absX: Int, absY: Int): RenderTreeNode = RenderTreeNode(
      parent = null,
      renderUnit = mock<RenderUnit<Any>>(),
      layoutData = null,
      bounds = Rect(absX, absY, absX + 100, absY + 100),
      resolvedPadding = null,
      positionInParent = 0,
  )

  @Test
  fun `matches when both absolute coordinates equal`() {
    assertThat(
        LithoReducer.externalParentMatches(
            snapshot(absX = 10, absY = 20),
            rtn(absX = 10, absY = 20),
        ),
    )
        .isTrue
  }

  @Test
  fun `rejects when absoluteX differs (parent moved horizontally)`() {
    assertThat(
        LithoReducer.externalParentMatches(
            snapshot(absX = 10, absY = 20),
            rtn(absX = 30, absY = 20),
        ),
    )
        .isFalse
  }

  @Test
  fun `rejects when absoluteY differs (sibling above grew, parent shifted down)`() {
    assertThat(
        LithoReducer.externalParentMatches(
            snapshot(absX = 10, absY = 20),
            rtn(absX = 10, absY = 200),
        ),
    )
        .isFalse
  }

  @Test
  fun `rejects when both coordinates differ`() {
    assertThat(
        LithoReducer.externalParentMatches(
            snapshot(absX = 10, absY = 20),
            rtn(absX = 30, absY = 200),
        ),
    )
        .isFalse
  }

  // The absolute position fields default to 0 in [SubtreeSnapshot]'s primary constructor for
  // backward compat. A parent whose absoluteX/Y are also 0 must still match (the default value
  // is not a sentinel for "unknown").
  @Test
  fun `matches when both snapshot defaults and parent are at origin`() {
    assertThat(
        LithoReducer.externalParentMatches(
            snapshot(absX = 0, absY = 0),
            rtn(absX = 0, absY = 0),
        ),
    )
        .isTrue
  }
}
