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
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.yoga.YogaEdge
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

@RunWith(LithoTestRunner::class)
class DeprecatedLithoTooltipTest {

  private lateinit var context: ComponentContext
  private lateinit var component: Component

  @Mock lateinit var lithoTooltip: DeprecatedLithoTooltip
  private lateinit var componentTree: ComponentTree
  private lateinit var lithoView: LithoView
  private lateinit var anchorGlobalKey: String

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    component =
        object : InlineLayoutSpec() {
          @OnCreateLayout
          override fun onCreateLayout(c: ComponentContext): Component =
              Row.create(c)
                  .marginPx(YogaEdge.LEFT, MARGIN_LEFT)
                  .marginPx(YogaEdge.TOP, MARGIN_TOP)
                  .child(
                      SimpleMountSpecTester.create(c)
                          .key(KEY_ANCHOR)
                          .widthPx(ANCHOR_WIDTH)
                          .heightPx(ANCHOR_HEIGHT))
                  .build()
        }
    componentTree = ComponentTree.create(context, component).build()
    context = ComponentContextUtils.withComponentTree(context, componentTree)
    context = ComponentContext.withComponentScope(context, component, component.key)
    lithoView = getLithoView(componentTree)
    anchorGlobalKey =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            Row.create(context).build().typeId, "$${KEY_ANCHOR}")
  }

  @Test
  fun testBottomLeft() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.BOTTOM_LEFT)
    verify(lithoTooltip)
        .showBottomLeft(lithoView, MARGIN_LEFT, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT)
  }

  @Test
  fun testCenterBottom() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.CENTER_BOTTOM)
    verify(lithoTooltip)
        .showBottomLeft(
            lithoView, MARGIN_LEFT + ANCHOR_WIDTH / 2, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT)
  }

  @Test
  fun testBottomRight() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.BOTTOM_RIGHT)
    verify(lithoTooltip)
        .showBottomLeft(
            lithoView, MARGIN_LEFT + ANCHOR_WIDTH, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT)
  }

  @Test
  fun testCenterRight() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.CENTER_RIGHT)
    verify(lithoTooltip)
        .showBottomLeft(
            lithoView, MARGIN_LEFT + ANCHOR_WIDTH, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT / 2)
  }

  @Test
  fun testTopRight() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.TOP_RIGHT)
    verify(lithoTooltip)
        .showBottomLeft(lithoView, MARGIN_LEFT + ANCHOR_WIDTH, -HOST_HEIGHT + MARGIN_TOP)
  }

  @Test
  fun testCenterTop() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.CENTER_TOP)
    verify(lithoTooltip)
        .showBottomLeft(lithoView, MARGIN_LEFT + ANCHOR_WIDTH / 2, -HOST_HEIGHT + MARGIN_TOP)
  }

  @Test
  fun testTopLeft() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.TOP_LEFT)
    verify(lithoTooltip).showBottomLeft(lithoView, MARGIN_LEFT, -HOST_HEIGHT + MARGIN_TOP)
  }

  @Test
  fun testCenterLeft() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.CENTER_LEFT)
    verify(lithoTooltip)
        .showBottomLeft(lithoView, MARGIN_LEFT, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT / 2)
  }

  @Test
  fun testCenter() {
    LithoTooltipController.showTooltip(
        context, lithoTooltip, anchorGlobalKey, TooltipPosition.CENTER)
    verify(lithoTooltip)
        .showBottomLeft(
            lithoView,
            MARGIN_LEFT + ANCHOR_WIDTH / 2,
            -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT / 2)
  }

  private fun getLithoView(componentTree: ComponentTree?): LithoView {
    val lithoView = LithoView(context)
    lithoView.componentTree = componentTree
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(HOST_WIDTH, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(HOST_HEIGHT, View.MeasureSpec.EXACTLY))
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return lithoView
  }

  companion object {
    private const val HOST_WIDTH = 400
    private const val HOST_HEIGHT = 300
    private const val ANCHOR_WIDTH = 200
    private const val ANCHOR_HEIGHT = 100
    private const val MARGIN_LEFT = 20
    private const val MARGIN_TOP = 10
    private const val KEY_ANCHOR = "anchor"
  }
}
