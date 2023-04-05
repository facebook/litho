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

import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LithoNodeResolvedPaddingTest {

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()
  private lateinit var builder: Component.Builder<*>

  @Before
  fun setup() {
    val context = legacyLithoViewRule.context
    builder = Column.create(context)
  }

  private fun padding(edge: YogaEdge, padding: Int): LithoNodeResolvedPaddingTest {
    builder.paddingPx(edge, padding)
    return this
  }

  private fun direction(direction: YogaDirection): LithoNodeResolvedPaddingTest {
    builder.layoutDirection(direction)
    return this
  }

  private fun calculateLayout(): LithoLayoutResult? =
      legacyLithoViewRule
          .attachToWindow()
          .setRoot(builder.build())
          .measure()
          .layout()
          .currentRootNode

  @Test
  fun testPaddingLeftWithUndefinedStartEnd() {
    val result = padding(YogaEdge.LEFT, 10).direction(YogaDirection.LTR).calculateLayout()
    assertThat(result?.paddingLeft).isEqualTo(10)
  }

  @Test
  fun testPaddingLeftWithDefinedStart() {
    val result =
        padding(YogaEdge.START, 5)
            .padding(YogaEdge.LEFT, 10)
            .direction(YogaDirection.LTR)
            .calculateLayout()
    assertThat(result?.paddingLeft).isEqualTo(5)
  }

  @Test
  fun testPaddingLeftWithDefinedEnd() {
    val result =
        padding(YogaEdge.END, 5)
            .padding(YogaEdge.LEFT, 10)
            .direction(YogaDirection.LTR)
            .calculateLayout()
    assertThat(result?.paddingLeft).isEqualTo(10)
  }

  @Test
  fun testPaddingLeftWithDefinedStartInRtl() {
    val result =
        padding(YogaEdge.START, 5)
            .padding(YogaEdge.LEFT, 10)
            .direction(YogaDirection.RTL)
            .calculateLayout()
    assertThat(result?.paddingLeft).isEqualTo(10)
  }

  @Test
  fun testPaddingLeftWithDefinedEndInRtl() {
    val result =
        padding(YogaEdge.END, 5)
            .padding(YogaEdge.LEFT, 10)
            .direction(YogaDirection.RTL)
            .calculateLayout()
    assertThat(result?.paddingLeft).isEqualTo(5)
  }

  @Test
  fun testPaddingRightWithUndefinedStartEnd() {
    val result = padding(YogaEdge.RIGHT, 10).direction(YogaDirection.LTR).calculateLayout()
    assertThat(result?.paddingRight).isEqualTo(10)
  }

  @Test
  fun testPaddingRightWithDefinedStart() {
    padding(YogaEdge.START, 5).padding(YogaEdge.RIGHT, 10).direction(YogaDirection.LTR)
    val result = calculateLayout()
    assertThat(result?.paddingRight).isEqualTo(10)
  }

  @Test
  fun testPaddingRightWithDefinedEnd() {
    val result =
        padding(YogaEdge.END, 5)
            .padding(YogaEdge.RIGHT, 10)
            .direction(YogaDirection.LTR)
            .calculateLayout()
    assertThat(result?.paddingRight).isEqualTo(5)
  }

  @Test
  fun testPaddingRightWithDefinedStartInRtl() {
    val result =
        padding(YogaEdge.START, 5)
            .padding(YogaEdge.RIGHT, 10)
            .direction(YogaDirection.RTL)
            .calculateLayout()
    assertThat(result?.paddingRight).isEqualTo(5)
  }

  @Test
  fun testPaddingRightWithDefinedEndInRtl() {
    val result =
        padding(YogaEdge.END, 5)
            .padding(YogaEdge.RIGHT, 10)
            .direction(YogaDirection.RTL)
            .calculateLayout()
    assertThat(result?.paddingRight).isEqualTo(10)
  }
}
