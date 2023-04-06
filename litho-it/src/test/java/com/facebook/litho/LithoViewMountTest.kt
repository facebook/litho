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
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.MountState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Tests for [LithoView] and [MountState] to make sure mount only happens once when attaching the
 * view and setting the component.
 */
@RunWith(LithoTestRunner::class)
class LithoViewMountTest {

  private lateinit var context: ComponentContext
  private lateinit var lithoView: TestLithoView
  private lateinit var component: Component
  private lateinit var componentTree: ComponentTree

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    lithoView = spy(TestLithoView(context.androidContext))
    lithoView.resetRequestLayoutInvocationCount()
    component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              SimpleMountSpecTester.create(c).widthPx(100).heightPx(100).build()
        }
    componentTree =
        createComponentTree(useSpy = false, incMountEnabled = false, width = 100, height = 100)
  }

  @Test
  fun testIncrementalMountTriggeredAfterUnmountAllWithSameDimensions() {
    componentTree =
        createComponentTree(useSpy = true, incMountEnabled = true, width = 100, height = 100)
    val width = 50
    val height = 50
    lithoView.setMeasured(width, height)
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    verify(lithoView).performIncrementalMountForVisibleBoundsChange()
    lithoView.unmountAllItems()
    lithoView.performLayout(false, 0, 0, width, height)
    verify(lithoView, times(2)).performIncrementalMountForVisibleBoundsChange()
  }

  @Test
  fun testOnlyRequestLayoutCalledUntilMeasured() {
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(1)
  }

  @Test
  fun testSetComponentAndAttachRequestsLayout() {
    lithoView.setMeasured(10, 10)
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(2)
  }

  @Test
  fun testSetSameSizeComponentAndAttachRequestsLayout() {
    lithoView.setMeasured(100, 100)
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(2)
  }

  @Test
  fun testSetComponentTwiceWithResetAndAttachRequestsLayout() {
    val ct = createComponentTree(useSpy = false, incMountEnabled = false, width = 100, height = 100)
    lithoView.componentTree = ct
    lithoView.setMeasured(100, 100)
    lithoView.onAttachedToWindow()
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(2)
    lithoView.onDetachedFromWindow()
    lithoView.resetRequestLayoutInvocationCount()
    lithoView.componentTree = ct
    lithoView.onAttachedToWindow()
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(1)
  }

  @Test
  fun testAttachAndSetSameSizeComponentRequestsLayout() {
    lithoView.setMeasured(100, 100)
    lithoView.onAttachedToWindow()
    lithoView.componentTree = componentTree
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(1)
  }

  @Test
  fun testAttachAndSetComponentRequestsLayout() {
    lithoView.setMeasured(10, 10)
    lithoView.onAttachedToWindow()
    lithoView.componentTree = componentTree
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(1)
  }

  @Test
  fun testReAttachRequestsLayout() {
    lithoView.setMeasured(100, 100)
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(2)
    lithoView.onDetachedFromWindow()
    lithoView.resetRequestLayoutInvocationCount()
    lithoView.onAttachedToWindow()
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(1)
    val newComponentTree =
        createComponentTree(useSpy = false, incMountEnabled = false, width = 100, height = 100)
    lithoView.resetRequestLayoutInvocationCount()
    lithoView.componentTree = newComponentTree
    assertThat(lithoView.requestLayoutInvocationCount).isEqualTo(1)
  }

  @Test
  fun testSetHasTransientStateMountsEverythingIfIncrementalMountEnabled() {
    val child1 = TestViewComponent.create(context).build()
    val lifecycleTracker2 = LifecycleTracker()
    val child2 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val lithoView: LithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                      .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                      .build()
            },
            true,
            true)
    lithoView.notifyVisibleBoundsChanged(Rect(0, -10, 10, -5), true)
    assertThat(child1.isMounted).isFalse
    assertThat(lifecycleTracker2.isMounted).isFalse
    lithoView.setHasTransientState(true)
    assertThat(child1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isTrue
  }

  @Test
  fun testUnmountAllCausesRemountOfComponentTreeOnLayout() {
    val child1 = TestViewComponent.create(context).build()
    val lifecycleTracker2 = LifecycleTracker()
    val child2 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val lithoView: LithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                      .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                      .build()
            },
            true,
            true)
    lithoView.performLayout(false, 0, 0, 100, 100)
    assertThat(child1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isTrue
    lithoView.unmountAllItems()
    assertThat(child1.isMounted).isFalse
    assertThat(lifecycleTracker2.isMounted).isFalse
    lithoView.performLayout(false, 0, 0, 100, 100)
    assertThat(child1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isTrue
  }

  @Test
  fun testPerformLayoutWithDifferentBoundsMountsEverything() {
    val child1 = TestViewComponent.create(context).build()
    val lifecycleTracker2 = LifecycleTracker()
    val child2 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val lithoView: LithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                      .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                      .build()
            },
            true,
            true)
    lithoView.notifyVisibleBoundsChanged(Rect(0, -10, 10, -5), true)
    assertThat(child1.isMounted).isFalse
    assertThat(lifecycleTracker2.isMounted).isFalse
    lithoView.performLayout(false, 0, 0, 200, 200)
    lithoView.viewTreeObserver.dispatchOnGlobalLayout()
    assertThat(child1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isTrue
  }

  private fun createComponentTree(
      useSpy: Boolean,
      incMountEnabled: Boolean,
      width: Int,
      height: Int
  ): ComponentTree {
    val componentTree =
        ComponentTree.create(context, component)
            .incrementalMount(incMountEnabled)
            .layoutDiffing(false)
            .build()
    componentTree.setSizeSpec(
        SizeSpec.makeSizeSpec(width, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(height, SizeSpec.EXACTLY))
    return if (useSpy) spy(componentTree) else componentTree
  }

  private open class TestLithoView(context: Context?) : LithoView(context) {
    var requestLayoutInvocationCount = 0
      private set

    override fun requestLayout() {
      super.requestLayout()
      requestLayoutInvocationCount++
    }

    fun resetRequestLayoutInvocationCount() {
      requestLayoutInvocationCount = 0
    }

    fun setMeasured(width: Int, height: Int) {
      setMeasuredDimension(width, height)
    }
  }
}
