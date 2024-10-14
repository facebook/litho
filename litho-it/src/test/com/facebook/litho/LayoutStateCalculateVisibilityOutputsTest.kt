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

import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLayoutComponent
import com.facebook.litho.testing.eventhandler.EventHandlerTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.TestNullLayoutComponent
import com.facebook.rendercore.MountContentPools
import com.facebook.yoga.YogaEdge
import java.lang.Exception
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LayoutStateCalculateVisibilityOutputsTest {
  @JvmField @Rule val mLithoTestRule: LithoTestRule = LithoTestRule()

  @Before
  @Throws(Exception::class)
  fun setup() {
    MountContentPools.clear()
  }

  @Test
  fun testNoUnnecessaryVisibilityOutputs() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .child(
                            SimpleMountSpecTester.create(c)
                                .visibleHandler(
                                    EventHandlerTestHelper.create(c, 1)
                                        as EventHandler<VisibleEvent>)))
                .child(
                    SimpleMountSpecTester.create(c)
                        .invisibleHandler(
                            EventHandlerTestHelper.create(c, 2) as EventHandler<InvisibleEvent>))
                .child(SimpleMountSpecTester.create(c))
                .build()
          }
        }
    val layoutState = calculateLayoutState(component, 100, 100)
    Assertions.assertThat(layoutState.visibilityOutputCount).isEqualTo(2)
  }

  @Test
  fun testNoUnnecessaryVisibilityOutputsWithFullImpression() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .child(
                            SimpleMountSpecTester.create(c)
                                .visibleHandler(
                                    EventHandlerTestHelper.create(c, 1)
                                        as EventHandler<VisibleEvent>)))
                .child(
                    SimpleMountSpecTester.create(c)
                        .fullImpressionHandler(
                            EventHandlerTestHelper.create(c, 3)
                                as EventHandler<FullImpressionVisibleEvent>))
                .child(SimpleMountSpecTester.create(c))
                .build()
          }
        }
    val layoutState = calculateLayoutState(component, 100, 100)
    Assertions.assertThat(layoutState.visibilityOutputCount).isEqualTo(2)
  }

  @Test
  fun testNoUnnecessaryVisibilityOutputsWithFocused() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .child(
                            SimpleMountSpecTester.create(c)
                                .visibleHandler(
                                    EventHandlerTestHelper.create(c, 1)
                                        as EventHandler<VisibleEvent>)))
                .child(
                    SimpleMountSpecTester.create(c)
                        .focusedHandler(
                            EventHandlerTestHelper.create(c, 4)
                                as EventHandler<FocusedVisibleEvent>))
                .child(SimpleMountSpecTester.create(c))
                .build()
          }
        }
    val layoutState = calculateLayoutState(component, 100, 100)
    Assertions.assertThat(layoutState.visibilityOutputCount).isEqualTo(2)
  }

  @Test
  fun testVisibilityOutputsForDelegateComponents() {
    val isDelegate = true
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(
                    TestLayoutComponent.create(c, 0, 0, true, false, isDelegate)
                        .visibleHandler(
                            EventHandlerTestHelper.create(c, 1) as EventHandler<VisibleEvent>))
                .wrapInView()
                .build()
          }
        }
    val layoutState = calculateLayoutState(component, 100, 100)
    Assertions.assertThat(layoutState.visibilityOutputCount).isEqualTo(1)
  }

  @Test
  fun testLayoutOutputsForDeepLayoutSpecs() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .child(
                            TestLayoutComponent.create(c)
                                .visibleHandler(
                                    EventHandlerTestHelper.create(c, 1)
                                        as EventHandler<VisibleEvent>))
                        .invisibleHandler(
                            EventHandlerTestHelper.create(c, 2) as EventHandler<InvisibleEvent>))
                .child(
                    Column.create(c)
                        .child(
                            TestLayoutComponent.create(c)
                                .invisibleHandler(
                                    EventHandlerTestHelper.create(c, 1)
                                        as EventHandler<InvisibleEvent>))
                        .visibleHandler(
                            EventHandlerTestHelper.create(c, 2) as EventHandler<VisibleEvent>))
                .wrapInView()
                .build()
          }
        }
    val layoutState = calculateLayoutState(component, 350, 200)

    // Check total layout outputs.
    Assertions.assertThat(layoutState.visibilityOutputCount).isEqualTo(4)

    // Check number of Components with VisibleEvent handlers.
    var visibleHandlerCount = 0
    for (i in 0 until layoutState.visibilityOutputCount) {
      if (layoutState.getVisibilityOutputAt(i).visibleEventHandler != null) {
        visibleHandlerCount += 1
      }
    }
    Assertions.assertThat(visibleHandlerCount).isEqualTo(2)
  }

  @Test
  fun testLayoutOutputsForForceWrappedComponent() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(
                    SimpleMountSpecTester.create(c)
                        .visibleHandler(
                            EventHandlerTestHelper.create(c, 1) as EventHandler<VisibleEvent>)
                        .wrapInView())
                .build()
          }
        }
    val layoutState = calculateLayoutState(component, 100, 100)
    Assertions.assertThat(layoutState.visibilityOutputCount).isEqualTo(1)
  }

  @Test
  fun testLayoutOutputForRootWithNullLayout() {
    val componentWithNullLayout: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component? {
            return null
          }
        }
    val layoutState = calculateLayoutState(componentWithNullLayout, 350, 200)
    Assertions.assertThat(layoutState.visibilityOutputCount).isEqualTo(0)
  }

  @Test
  fun testLayoutComponentForNestedTreeChildWithNullLayout() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .paddingPx(YogaEdge.ALL, 2)
                .child(TestNullLayoutComponent.create(c))
                .invisibleHandler(
                    EventHandlerTestHelper.create(c, 2) as EventHandler<InvisibleEvent>)
                .build()
          }
        }
    val layoutState = calculateLayoutState(component, 350, 200)
    Assertions.assertThat(layoutState.visibilityOutputCount).isEqualTo(1)
  }

  private fun calculateLayoutState(component: Component, widthPx: Int, heightPx: Int): LayoutState {
    val testLithoView = mLithoTestRule.render(widthPx = widthPx, heightPx = heightPx) { component }
    return testLithoView.componentTree.mainThreadLayoutState!!
  }
}
