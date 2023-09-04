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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LithoNodeTouchExpansionTest {

  private lateinit var node: LithoNode
  lateinit var context: ComponentContext
  private lateinit var lithoLayoutContext: LithoLayoutContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val resolveContext = context.setRenderStateContextForTests()
    node = requireNotNull(Resolver.resolve(resolveContext, context, Column.create(context).build()))
    node.mutableNodeInfo().touchHandler = EventHandler(null, 1)
    lithoLayoutContext =
        LithoLayoutContext(
            resolveContext.treeId,
            resolveContext.cache,
            context,
            resolveContext.treeState,
            resolveContext.layoutVersion,
            resolveContext.rootComponentId,
            resolveContext.isAccessibilityEnabled,
            LayoutCache(),
            null,
            null)
  }

  private fun setDirection(direction: YogaDirection): LithoNodeTouchExpansionTest {
    node.layoutDirection(direction)
    return this
  }

  private fun touchExpansionPx(edge: YogaEdge, value: Int): LithoNodeTouchExpansionTest {
    node.touchExpansionPx(edge, value)
    return this
  }

  private fun calculateLayout(): LithoLayoutResult? {
    val context =
        LayoutContext(
            context.androidContext, LithoRenderContext(lithoLayoutContext), 0, LayoutCache(), null)
    return node.calculateLayout(context, SizeSpec.UNSPECIFIED, SizeSpec.UNSPECIFIED)
  }

  @Test
  fun testTouchExpansionLeftWithoutTouchHandling() {
    node.mutableNodeInfo().touchHandler = null
    val result = touchExpansionPx(YogaEdge.LEFT, 10).calculateLayout()
    assertThat(result?.touchExpansionLeft).isEqualTo(0)
  }

  @Test
  fun testTouchExpansionTopWithoutTouchHandling() {
    node.mutableNodeInfo().touchHandler = null
    val result = touchExpansionPx(YogaEdge.TOP, 10).calculateLayout()
    assertThat(result?.touchExpansionTop).isEqualTo(0)
  }

  @Test
  fun testTouchExpansionRightWithoutTouchHandling() {
    node.mutableNodeInfo().touchHandler = null
    val result = touchExpansionPx(YogaEdge.RIGHT, 10).calculateLayout()
    assertThat(result?.touchExpansionRight).isEqualTo(0)
  }

  @Test
  fun testTouchExpansionBottomWithoutTouchHandling() {
    node.mutableNodeInfo().touchHandler = null
    val result = touchExpansionPx(YogaEdge.BOTTOM, 10).calculateLayout()
    assertThat(result?.touchExpansionBottom).isEqualTo(0)
  }

  @Test
  fun testTouchExpansionLeftWithUndefinedStartEnd() {
    val result = touchExpansionPx(YogaEdge.LEFT, 10).calculateLayout()
    assertThat(result?.touchExpansionLeft).isEqualTo(10)
  }

  @Test
  fun testTouchExpansionLeftWithDefinedStart() {
    val result =
        touchExpansionPx(YogaEdge.START, 5).touchExpansionPx(YogaEdge.LEFT, 10).calculateLayout()
    assertThat(result?.touchExpansionLeft).isEqualTo(5)
  }

  @Test
  fun testTouchExpansionLeftWithDefinedEnd() {
    val result =
        touchExpansionPx(YogaEdge.END, 5).touchExpansionPx(YogaEdge.LEFT, 10).calculateLayout()
    assertThat(result?.touchExpansionLeft).isEqualTo(10)
  }

  @Test
  fun testTouchExpansionLeftWithDefinedStartInRtl() {
    val result =
        setDirection(YogaDirection.RTL)
            .touchExpansionPx(YogaEdge.START, 5)
            .touchExpansionPx(YogaEdge.LEFT, 10)
            .calculateLayout()
    assertThat(result?.touchExpansionLeft).isEqualTo(10)
  }

  @Test
  fun testTouchExpansionLeftWithDefinedEndInRtl() {
    val result =
        setDirection(YogaDirection.RTL)
            .touchExpansionPx(YogaEdge.END, 5)
            .touchExpansionPx(YogaEdge.LEFT, 10)
            .calculateLayout()
    assertThat(result?.touchExpansionLeft).isEqualTo(5)
  }

  @Test
  fun testTouchExpansionRightWithUndefinedStartEnd() {
    val result = touchExpansionPx(YogaEdge.RIGHT, 10).calculateLayout()
    assertThat(result?.touchExpansionRight).isEqualTo(10)
  }

  @Test
  fun testTouchExpansionRightWithDefinedStart() {
    val result =
        touchExpansionPx(YogaEdge.START, 5).touchExpansionPx(YogaEdge.RIGHT, 10).calculateLayout()
    assertThat(result?.touchExpansionRight).isEqualTo(10)
  }

  @Test
  fun testTouchExpansionRightWithDefinedEnd() {
    val result =
        touchExpansionPx(YogaEdge.END, 5).touchExpansionPx(YogaEdge.RIGHT, 10).calculateLayout()
    assertThat(result?.touchExpansionRight).isEqualTo(5)
  }

  @Test
  fun testTouchExpansionRightWithDefinedStartInRtl() {
    val result =
        setDirection(YogaDirection.RTL)
            .touchExpansionPx(YogaEdge.START, 5)
            .touchExpansionPx(YogaEdge.RIGHT, 10)
            .calculateLayout()
    assertThat(result?.touchExpansionRight).isEqualTo(5)
  }

  @Test
  fun testTouchExpansionRightWithDefinedEndInRtl() {
    val result =
        setDirection(YogaDirection.RTL)
            .touchExpansionPx(YogaEdge.END, 5)
            .touchExpansionPx(YogaEdge.RIGHT, 10)
            .calculateLayout()
    assertThat(result?.touchExpansionRight).isEqualTo(10)
  }
}
