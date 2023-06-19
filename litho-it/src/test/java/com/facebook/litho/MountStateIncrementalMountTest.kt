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
import android.content.Context.ACCESSIBILITY_SERVICE
import android.graphics.Rect
import android.os.Looper
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DynamicComponentGroupSection
import com.facebook.litho.sections.common.SingleComponentSection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.ViewGroupWithLithoViewChildren
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.ComponentTreeHolder
import com.facebook.litho.widget.LithoViewFactory
import com.facebook.litho.widget.MountSpecExcludeFromIncrementalMount
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.MountSpecLifecycleTesterDrawable
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.SimpleStateUpdateEmulator
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec
import com.facebook.litho.widget.Text
import com.facebook.rendercore.utils.MeasureSpecUtils.exactly
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.validateMockitoUsage
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class MountStateIncrementalMountTest {

  private lateinit var context: ComponentContext
  private lateinit var resolveThreadShadowLooper: ShadowLooper
  private lateinit var layoutThreadShadowLooper: ShadowLooper

  @JvmField
  @Rule
  val legacyLithoViewRule =
      LegacyLithoViewRule(
          ComponentsConfiguration.create().shouldAddHostViewForRootComponent(true).build())

  @Before
  fun setup() {
    context = legacyLithoViewRule.context
    legacyLithoViewRule.useLithoView(LithoView(context))
    layoutThreadShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultLayoutThreadLooper")
                as Looper)
    resolveThreadShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultResolveThreadLooper")
                as Looper)
  }

  @After
  fun restoreConfiguration() {
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    validateMockitoUsage()
  }

  private fun runToEndOfTasks() {
    resolveThreadShadowLooper.runToEndOfTasks()
    layoutThreadShadowLooper.runToEndOfTasks()
  }

  @Test
  fun testExcludeFromIncrementalMountForInvisibleComponents() {

    // ┌─────────────┬────────┐
    // │Visible Rect │        │
    // ├─────────────┘        │
    // │ ┌────────────┬─────┐ │
    // │ │ Component1 │     │ │
    // │ ├────────────┘     │ │
    // │ │                  │ │
    // │ └──────────────────┘ │
    // └──────────────────────┘
    //   ┌────────────┬─────┐
    //   │ Component2 │     │
    //   ├────────────┘     │
    //   │                  │
    //   └──────────────────┘
    //   ┌────────────┬─────┐
    //   │ Component3 │     │
    //   ├────────────┘     │
    //   │                  │
    //   └──────────────────┘
    val eventHandler1: EventHandler<VisibleEvent> = mock()
    val eventHandler2: EventHandler<VisibleEvent> = mock()

    // Component1 without `excludeFromIncrementalMount`
    val tracker1 = LifecycleTracker()
    val component1 =
        MountSpecLifecycleTester.create(context)
            .widthPx(100)
            .heightPx(30)
            .lifecycleTracker(tracker1)
            .build()

    // Component2 marked with `excludeFromIncrementalMount`
    val tracker2 = LifecycleTracker()
    val component2 =
        MountSpecExcludeFromIncrementalMount.create(context)
            .widthPx(100)
            .heightPx(30)
            .lifecycleTracker(tracker2)
            .build()

    // Component3 without `excludeFromIncrementalMount`
    val tracker3 = LifecycleTracker()
    val component3 =
        MountSpecLifecycleTester.create(context)
            .widthPx(100)
            .heightPx(30)
            .lifecycleTracker(tracker3)
            .build()

    // add a RootHost to check if the state of [excludeFromIncrementalMount]
    // propagates up to its parent
    val root =
        Wrapper.create(context)
            .delegate(
                Column.create(context)
                    .child(
                        Wrapper.create(context).delegate(component1).visibleHandler(eventHandler1))
                    .child(
                        Wrapper.create(context).delegate(component2).visibleHandler(eventHandler2))
                    .child(component3)
                    .build())
            .wrapInView()
            .build()
    legacyLithoViewRule.attachToWindow().setSizeSpecs(exactly(100), exactly(100)).measure()
    val info = ComponentRenderInfo.create().component(root).build()
    val holder = ComponentTreeHolder.create().renderInfo(info).build()
    holder.computeLayoutSync(context, exactly(100), exactly(100), Size())
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.componentTree = holder.componentTree
    lithoView.mountComponent(Rect(0, 0, 100, 30), true)
    assertThat(tracker1.isMounted)
        .describedAs("Visible component WITHOUT excludeFromIM should get mounted")
        .isTrue
    assertThat(tracker2.isMounted)
        .describedAs("Invisible component WITH excludeFromIM should get mounted")
        .isTrue
    assertThat(tracker3.isMounted)
        .describedAs("Invisible component WITHOUT excludeFromIM should Not get mounted")
        .isFalse
    // verify that the visibility callback of the visible component should be called
    verify(eventHandler1, times(1)).call(anyOrNull<VisibleEvent>())
    // verify that the visibility callback of the invisible component should not be called
    verify(eventHandler2, times(0)).call(anyOrNull<VisibleEvent>())

    // move the view out of visible area and make sure the component that marked as excludeFromIM
    // will not get unmounted
    lithoView.notifyVisibleBoundsChanged(Rect(0, -50, 100, -10), true)
    assertThat(tracker1.isMounted)
        .describedAs("Invisible component WITHOUT excludeFromIM should get unmounted")
        .isFalse
    assertThat(tracker2.isMounted)
        .describedAs("Invisible component WITH excludeFromIM should Not get unmounted")
        .isTrue
    assertThat(tracker3.isMounted)
        .describedAs("Invisible component WITHOUT excludeFromIM should get mounted")
        .isFalse
    // verify that the visibility callback of the invisible component should not be called
    verify(eventHandler1, times(1)).call(anyOrNull<VisibleEvent>())
    verify(eventHandler2, times(0)).call(anyOrNull<VisibleEvent>())
    lithoView.notifyVisibleBoundsChanged(Rect(0, 15, 100, 45), true)
    // verify that the visibility callback of the visible component should be called
    verify(eventHandler1, times(2)).call(anyOrNull<VisibleEvent>())
    verify(eventHandler2, times(1)).call(anyOrNull<VisibleEvent>())
  }

  @Test
  fun testExcludeFromIncrementalMount() {
    val child = TestViewComponent.create(context).widthPx(100).heightPx(100).build()
    val skipIMTracker = LifecycleTracker()
    val excludeIMComponent =
        MountSpecExcludeFromIncrementalMount.create(context)
            .lifecycleTracker(skipIMTracker)
            .widthPx(100)
            .heightPx(100)
            .build()
    val notSkipIMTracker = LifecycleTracker()
    val doesNotExcludeIMComponent =
        MountSpecLifecycleTester.create(context)
            .lifecycleTracker(notSkipIMTracker)
            .widthPx(100)
            .heightPx(100)
            .build()
    val root =
        Column.create(context)
            .widthPercent(100f)
            .heightPercent(100f)
            .child(
                Column.create(context)
                    .child(child)
                    .child(doesNotExcludeIMComponent)
                    .child(excludeIMComponent))
            .build()
    val lithoView = LithoView(context)
    legacyLithoViewRule
        .useLithoView(lithoView)
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(500))
        .measure()
        .layout()
    lithoView.mountComponent(Rect(0, 400, 100, 500), false)
    assertThat(notSkipIMTracker.isMounted)
        .describedAs(
            "Component without excludeFromIncrementalMount doesn't get mounted when out of visible rect")
        .isFalse
    assertThat(skipIMTracker.isMounted)
        .describedAs(
            "Component with excludeFromIncrementalMount do get mounted when out of visible rect")
        .isTrue
    lithoView.mountComponent(Rect(0, 0, 100, 300), false)
    assertThat(notSkipIMTracker.isMounted)
        .describedAs(
            "Component without excludeFromIncrementalMount get mounted when in visible rect")
        .isTrue
    assertThat(skipIMTracker.isMounted)
        .describedAs("Component with excludeFromIncrementalMount get mounted when in visible rect")
        .isTrue
    lithoView.mountComponent(Rect(0, 400, 50, 450), false)
    assertThat(notSkipIMTracker.isMounted)
        .describedAs(
            "Component without excludeFromIncrementalMount get unmounted when out of visible rect")
        .isFalse
    assertThat(skipIMTracker.isMounted)
        .describedAs(
            "Component with excludeFromIncrementalMount doesn't get unmounted when out of visible rect")
        .isTrue
    lithoView.mountComponent(Rect(0, 400, 50, 450), false)
    assertThat(skipIMTracker.isMounted)
        .describedAs(
            "Component with excludeFromIncrementalMount doesn't get unmounted while doing IncrementalMount")
        .isTrue
  }

  /** Tests incremental mount behaviour of a vertical stack of components with a View mount type. */
  @Test
  fun testIncrementalMountVerticalViewStackScrollUp() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, -10, 10, -5), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 0, 10, 5), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 5, 10, 15), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isTrue
    lithoView.mountComponent(Rect(0, 15, 10, 25), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isTrue
    lithoView.mountComponent(Rect(0, 20, 10, 30), true)
    assertThat(child1.isMounted).isFalse

    // Inc-Mount-Ext will properly unmount items when their bottom is equal to the container's
    // top.
    assertThat(child2.isMounted).isFalse
  }

  @Test
  fun testIncrementalMountVerticalViewStackScrollDown() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 20, 10, 30), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 15, 10, 25), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isTrue
    lithoView.mountComponent(Rect(0, 5, 10, 15), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isTrue
    lithoView.mountComponent(Rect(0, 0, 10, 9), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, -10, 10, -5), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isFalse
  }

  @Test
  fun incrementalMount_visibleTopIntersectsItemBottom_unmountItem() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val child3 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child3).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 10, 10, 30), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isTrue
    assertThat(child3.isMounted).isTrue
  }

  @Test
  fun incrementalMount_visibleBottomIntersectsItemTop_unmountItem() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val child3 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child3).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 0, 10, 20), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isTrue
    assertThat(child3.isMounted).isFalse
  }

  @Test
  fun incrementalMount_visibleRectIntersectsItemBounds_mountItem() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val child3 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child3).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 10, 10, 20), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isTrue
    assertThat(child3.isMounted).isFalse
  }

  @Test
  fun incrementalMount_visibleBoundsEmpty_unmountAllItems() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val child3 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child3).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 0, 0, 0), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isFalse
    assertThat(child3.isMounted).isFalse
  }

  @Test
  fun incrementalMount_emptyItemBoundsIntersectVisibleRect_mountItem() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val child3 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(0))
            .child(Wrapper.create(context).delegate(child3).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 0, 10, 30), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isTrue
    assertThat(child3.isMounted).isTrue
  }

  @Test
  fun incrementalMount_emptyItemBoundsEmptyVisibleRect_unmountItem() {
    val child1 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(0))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 0, 10, 0), true)
    assertThat(child1.isMounted).isFalse
  }

  /**
   * Tests incremental mount behaviour of a horizontal stack of components with a View mount type.
   */
  @Test
  fun testIncrementalMountHorizontalViewStack() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val root =
        Row.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(-10, 0, -5, 10), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 0, 5, 10), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isFalse
    lithoView.mountComponent(Rect(5, 0, 15, 10), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isTrue
    lithoView.mountComponent(Rect(15, 0, 25, 10), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isTrue
    lithoView.mountComponent(Rect(20, 0, 30, 10), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isFalse
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a Drawable mount type.
   */
  @Test
  fun testIncrementalMountVerticalDrawableStack() {
    val lifecycleTracker1 = LifecycleTracker()
    val child1 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker1).build()
    val lifecycleTracker2 = LifecycleTracker()
    val child2 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, -10, 10, -5), true)
    assertThat(lifecycleTracker1.isMounted).isFalse
    assertThat(lifecycleTracker2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 0, 10, 5), true)
    assertThat(lifecycleTracker1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 5, 10, 15), true)
    assertThat(lifecycleTracker1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isTrue
    lithoView.mountComponent(Rect(0, 15, 10, 25), true)
    assertThat(lifecycleTracker1.isMounted).isFalse
    assertThat(lifecycleTracker2.isMounted).isTrue
    lithoView.mountComponent(Rect(0, 20, 10, 30), true)
    assertThat(lifecycleTracker1.isMounted).isFalse

    // Inc-Mount-Ext will properly unmount items when their bottom is equal to the container's
    // top.
    assertThat(lifecycleTracker2.isMounted).isFalse
  }

  /** Tests incremental mount behaviour of a view mount item in a nested hierarchy. */
  @Test
  fun testIncrementalMountNestedView() {
    val child = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .wrapInView()
            .paddingPx(YogaEdge.ALL, 20)
            .child(Wrapper.create(context).delegate(child).widthPx(10).heightPx(10))
            .child(SimpleMountSpecTester.create(context))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 0, 50, 20), true)
    assertThat(child.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 0, 50, 40), true)
    assertThat(child.isMounted).isTrue
    lithoView.mountComponent(Rect(30, 0, 50, 40), true)
    assertThat(child.isMounted).isFalse
  }

  /**
   * Verify that we can cope with a negative padding on a component that is wrapped in a view (since
   * the bounds of the component will be larger than the bounds of the view).
   */
  @Test
  @Ignore("T146174263")
  fun testIncrementalMountVerticalDrawableStackNegativeMargin() {
    // When self managing, LithoViews will not adhere to translation. Therefore components with
    // negative margins + translations will not be mounted, hence this test is not relevant
    // in this case.
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val parent = FrameLayout(context.androidContext)
    parent.measure(exactly(10), exactly(1_000))
    parent.layout(0, 0, 10, 1_000)
    legacyLithoViewRule
        .setRoot(Row.create(context).build())
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    parent.addView(lithoView)
    lithoView.translationY = 105f
    val eventHandler: EventHandler<ClickEvent> = mock()
    val lifecycleTracker1 = LifecycleTracker()
    val child1 =
        MountSpecLifecycleTesterDrawable.create(context).lifecycleTracker(lifecycleTracker1).build()
    val childHost1 =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(child1)
                    .widthPx(10)
                    .heightPx(10)
                    .clickHandler(eventHandler)
                    .marginDip(YogaEdge.TOP, -10f))
            .build()
    val rootHost =
        Row.create(context)
            .child(Wrapper.create(context).delegate(childHost1).clickHandler(eventHandler).build())
            .build()
    lithoView.componentTree?.root = rootHost
    assertThat(lifecycleTracker1.steps).contains(LifecycleStep.ON_MOUNT)
  }

  @Test
  @Ignore("T146174263")
  fun testIncrementalMountVerticalDrawableStackNegativeMargin_multipleUnmountedHosts() {
    // When self managing, LithoViews do not adhere to translation, and so items set with negative
    // margins won't be mounted.
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val parent = FrameLayout(context.androidContext)
    parent.measure(exactly(10), exactly(1_000))
    parent.layout(0, 0, 10, 1_000)
    legacyLithoViewRule
        .setRoot(Row.create(context).build())
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    parent.addView(lithoView)
    lithoView.translationY = 105f
    val eventHandler: EventHandler<ClickEvent> = mock()
    val lifecycleTracker1 = LifecycleTracker()
    val child1 =
        MountSpecLifecycleTesterDrawable.create(context).lifecycleTracker(lifecycleTracker1).build()
    val childHost1 =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(child1)
                    .widthPx(10)
                    .heightPx(10)
                    .clickHandler(eventHandler)
                    .marginDip(YogaEdge.TOP, -10f))
            .build()
    val rootHost =
        Row.create(context)
            .child(
                Row.create(context)
                    .viewTag("extra_host")
                    .child(
                        Wrapper.create(context)
                            .delegate(childHost1)
                            .clickHandler(eventHandler)
                            .build())
                    .child(
                        Wrapper.create(context)
                            .delegate(childHost1)
                            .clickHandler(eventHandler)
                            .build()))
            .build()
    lithoView.componentTree?.root = rootHost
    assertThat(lifecycleTracker1.steps).contains(LifecycleStep.ON_MOUNT)
  }

  @Test
  fun itemWithNegativeMargin_removeAndAdd_hostIsMounted() {
    val parent = FrameLayout(context.androidContext)
    parent.measure(exactly(10), exactly(1_000))
    parent.layout(0, 0, 10, 1_000)
    legacyLithoViewRule
        .setRoot(Row.create(context).build())
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    parent.addView(lithoView)
    lithoView.translationY = 95f
    val eventHandler1: EventHandler<ClickEvent> = mock()
    val lifecycleTracker1 = LifecycleTracker()
    val child1 =
        MountSpecLifecycleTesterDrawable.create(context).lifecycleTracker(lifecycleTracker1).build()
    val childHost1 =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(child1)
                    .widthPx(10)
                    .heightPx(10)
                    .clickHandler(eventHandler1))
            .build()
    val host1 =
        Row.create(context)
            .child(Wrapper.create(context).delegate(childHost1).clickHandler(eventHandler1).build())
            .build()
    val eventHandler2: EventHandler<ClickEvent> = mock()
    val lifecycleTracker2 = LifecycleTracker()
    val child2 =
        MountSpecLifecycleTesterDrawable.create(context).lifecycleTracker(lifecycleTracker2).build()
    val childHost2 =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(child2)
                    .widthPx(10)
                    .heightPx(10)
                    .clickHandler(eventHandler2)
                    .marginDip(YogaEdge.TOP, -10f))
            .build()
    val host2 =
        Row.create(context)
            .child(Wrapper.create(context).delegate(childHost2).clickHandler(eventHandler2).build())
            .build()
    val rootHost = Column.create(context).child(host1).child(host2).build()

    // Mount both child1 and child2.
    lithoView.componentTree?.root = rootHost
    assertThat(lifecycleTracker2.steps).contains(LifecycleStep.ON_MOUNT)
    lifecycleTracker2.reset()

    // Remove child2.
    val newHost = Column.create(context).child(host1).build()
    lithoView.componentTree?.root = newHost

    // Add child2 back.
    assertThat(lifecycleTracker2.steps).contains(LifecycleStep.ON_UNMOUNT)
    lifecycleTracker2.reset()
    lithoView.componentTree?.root = rootHost
    assertThat(lifecycleTracker2.steps).contains(LifecycleStep.ON_MOUNT)
  }

  /** Tests incremental mount behaviour of overlapping view mount items. */
  @Test
  fun testIncrementalMountOverlappingView() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(child1)
                    .positionType(YogaPositionType.ABSOLUTE)
                    .positionPx(YogaEdge.TOP, 0)
                    .positionPx(YogaEdge.LEFT, 0)
                    .widthPx(10)
                    .heightPx(10))
            .child(
                Wrapper.create(context)
                    .delegate(child2)
                    .positionType(YogaPositionType.ABSOLUTE)
                    .positionPx(YogaEdge.TOP, 5)
                    .positionPx(YogaEdge.LEFT, 5)
                    .widthPx(10)
                    .heightPx(10))
            .child(SimpleMountSpecTester.create(context))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 0, 5, 5), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isFalse
    lithoView.mountComponent(Rect(5, 5, 10, 10), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child2.isMounted).isTrue
    lithoView.mountComponent(Rect(10, 10, 15, 15), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isTrue
    lithoView.mountComponent(Rect(15, 15, 20, 20), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child2.isMounted).isFalse
  }

  @Test
  fun testChildViewGroupIncrementallyMounted() {
    // Incremental mounting works differently with self-managing LithoViews, so checking calls
    // to notifyVisibleBoundsChanged is not needed.
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val mountedView: ViewGroup = mock()
    whenever(mountedView.childCount).thenReturn(3)
    val childView1 = getMockLithoViewWithBounds(Rect(5, 10, 20, 30))
    whenever(mountedView.getChildAt(0)).thenReturn(childView1)
    val childView2 = getMockLithoViewWithBounds(Rect(10, 10, 50, 60))
    whenever(mountedView.getChildAt(1)).thenReturn(childView2)
    val childView3 = getMockLithoViewWithBounds(Rect(30, 35, 50, 60))
    whenever(mountedView.getChildAt(2)).thenReturn(childView3)
    val root = TestViewComponent.create(context).testView(mountedView).build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    verify(childView1).notifyVisibleBoundsChanged()
    verify(childView2).notifyVisibleBoundsChanged()
    verify(childView3).notifyVisibleBoundsChanged()
    reset(childView1)
    whenever(childView1.isIncrementalMountEnabled).thenReturn(true)
    reset(childView2)
    whenever(childView2.isIncrementalMountEnabled).thenReturn(true)
    reset(childView3)
    whenever(childView3.isIncrementalMountEnabled).thenReturn(true)
    lithoView.mountComponent(Rect(15, 15, 40, 40), true)
    verify(childView1, times(1)).notifyVisibleBoundsChanged()
    verify(childView2, times(1)).notifyVisibleBoundsChanged()
    verify(childView3, times(1)).notifyVisibleBoundsChanged()
  }

  @Test
  fun testChildViewGroupAllIncrementallyMountedNotProcessVisibilityOutputs() {
    // Incremental mounting works differently with self-managing LithoViews, so checking calls
    // to notifyVisibleBoundsChanged is not needed.
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val mountedView: ViewGroup = mock()
    whenever(mountedView.left).thenReturn(0)
    whenever(mountedView.top).thenReturn(0)
    whenever(mountedView.right).thenReturn(100)
    whenever(mountedView.bottom).thenReturn(100)
    whenever(mountedView.childCount).thenReturn(3)
    val childView1 = getMockLithoViewWithBounds(Rect(5, 10, 20, 30))
    whenever(childView1.translationX).thenReturn(5.0f)
    whenever(childView1.translationY).thenReturn(-10.0f)
    whenever(mountedView.getChildAt(0)).thenReturn(childView1)
    val childView2 = getMockLithoViewWithBounds(Rect(10, 10, 50, 60))
    whenever(mountedView.getChildAt(1)).thenReturn(childView2)
    val childView3 = getMockLithoViewWithBounds(Rect(30, 35, 50, 60))
    whenever(mountedView.getChildAt(2)).thenReturn(childView3)
    val root = TestViewComponent.create(context).testView(mountedView).build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView

    // Can't verify directly as the object will have changed by the time we get the chance to
    // verify it.
    doAnswer { invocation ->
          val rect = invocation.arguments[0] as Rect
          if (rect != Rect(0, 0, 15, 20)) {
            fail()
          }
          null
        }
        .`when`(childView1)
        .notifyVisibleBoundsChanged(anyOrNull<Rect>(), eq(true))
    doAnswer { invocation ->
          val rect = invocation.arguments[0] as Rect
          if (rect != Rect(0, 0, 40, 50)) {
            fail()
          }
          null
        }
        .`when`(childView2)
        .notifyVisibleBoundsChanged(anyOrNull<Rect>(), eq(true))
    doAnswer { invocation ->
          val rect = invocation.arguments[0] as Rect
          if (rect != Rect(0, 0, 20, 25)) {
            fail()
          }
          null
        }
        .`when`(childView3)
        .notifyVisibleBoundsChanged(anyOrNull<Rect>(), eq(true))
    verify(childView1).notifyVisibleBoundsChanged()
    verify(childView2).notifyVisibleBoundsChanged()
    verify(childView3).notifyVisibleBoundsChanged()
    reset(childView1)
    whenever(childView1.isIncrementalMountEnabled).thenReturn(true)
    reset(childView2)
    whenever(childView2.isIncrementalMountEnabled).thenReturn(true)
    reset(childView3)
    whenever(childView3.isIncrementalMountEnabled).thenReturn(true)
    lithoView.mountComponent(Rect(0, 0, 100, 100), true)
    verify(childView1, times(1)).notifyVisibleBoundsChanged()
    verify(childView2, times(1)).notifyVisibleBoundsChanged()
    verify(childView3, times(1)).notifyVisibleBoundsChanged()
  }

  /** Tests incremental mount behaviour of a vertical stack of components with a View mount type. */
  @Test
  fun testIncrementalMountDoesNotCauseMultipleUpdates() {
    val child1 = TestViewComponent.create(context).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, -10, 10, -5), true)
    assertThat(child1.isMounted).isFalse
    assertThat(child1.wasOnUnbindCalled()).isTrue
    assertThat(child1.wasOnUnmountCalled()).isTrue
    lithoView.mountComponent(Rect(0, 0, 10, 5), true)
    assertThat(child1.isMounted).isTrue
    child1.resetInteractions()
    lithoView.mountComponent(Rect(0, 5, 10, 15), true)
    assertThat(child1.isMounted).isTrue
    assertThat(child1.wasOnBindCalled()).isFalse
    assertThat(child1.wasOnMountCalled()).isFalse
    assertThat(child1.wasOnUnbindCalled()).isFalse
    assertThat(child1.wasOnUnmountCalled()).isFalse
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a Drawable mount type
   * after unmountAllItems was called.
   */
  @Test
  fun testIncrementalMountAfterUnmountAllItemsCall() {
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val child1 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker1).build()
    val child2 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, -10, 10, -5), true)
    assertThat(lifecycleTracker1.isMounted).isFalse
    assertThat(lifecycleTracker2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 0, 10, 5), true)
    assertThat(lifecycleTracker1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 5, 10, 15), true)
    assertThat(lifecycleTracker1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isTrue
    lithoView.unmountAllItems()
    assertThat(lifecycleTracker1.isMounted).isFalse
    assertThat(lifecycleTracker2.isMounted).isFalse
    lithoView.mountComponent(Rect(0, 5, 10, 15), true)
    assertThat(lifecycleTracker1.isMounted).isTrue
    assertThat(lifecycleTracker2.isMounted).isTrue
  }

  @Test
  fun testMountStateNeedsRemount_incrementalMountAfterUnmount_isFalse() {
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val child1 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker1).build()
    val child2 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    assertThat(lithoView.mountStateNeedsRemount()).isFalse
    lithoView.unmountAllItems()
    assertThat(lithoView.mountStateNeedsRemount()).isTrue
    lithoView.mountComponent(Rect(0, 5, 10, 15), true)
    assertThat(lithoView.mountStateNeedsRemount()).isFalse
  }

  @Test
  fun testRootViewAttributes_incrementalMountAfterUnmount_setViewAttributes() {
    enableAccessibility()
    val root = Text.create(context).text("Test").contentDescription("testcd").build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    var innerView = lithoView.getChildAt(0)
    assertThat(innerView.contentDescription).isEqualTo("testcd")
    lithoView.unmountAllItems()
    assertThat(innerView.contentDescription).isNull()
    lithoView.mountComponent(Rect(0, 5, 10, 15), true)
    innerView = lithoView.getChildAt(0)
    assertThat(innerView.contentDescription).isEqualTo("testcd")
  }

  /**
   * Tests incremental mount behaviour of a nested Litho View. We want to ensure that when a child
   * view is first mounted due to a layout pass it does not also have notifyVisibleBoundsChanged
   * called on it.
   */
  @Test
  fun testIncrementalMountAfterLithoViewIsMounted() {
    // Incremental mounting works differently with self-managing LithoViews, so checking calls
    // to notifyVisibleBoundsChanged is not needed.
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val lithoView: LithoView = mock()
    whenever(lithoView.isIncrementalMountEnabled).thenReturn(true)
    val viewGroup = ViewGroupWithLithoViewChildren(context.androidContext)
    viewGroup.addView(lithoView)
    val root = TestViewComponent.create(context, true, true, true).testView(viewGroup).build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoViewParent = legacyLithoViewRule.lithoView
    verify(lithoView).notifyVisibleBoundsChanged()
    reset(lithoView)

    // Mount views with visible rect
    lithoViewParent.mountComponent(Rect(0, 0, 100, 1_000), true)
    verify(lithoView, times(1)).notifyVisibleBoundsChanged()
    reset(lithoView)
    whenever(lithoView.isIncrementalMountEnabled).thenReturn(true)

    // Unmount views with visible rect outside
    lithoViewParent.mountComponent(Rect(0, -10, 100, -5), true)
    verify(lithoView, never()).notifyVisibleBoundsChanged()
    reset(lithoView)
    whenever(lithoView.isIncrementalMountEnabled).thenReturn(true)

    // Mount again with visible rect
    lithoViewParent.mountComponent(Rect(0, 0, 100, 1_000), true)
    verify(lithoView, times(1)).notifyVisibleBoundsChanged()
  }

  @Test
  fun incrementalMount_dirtyMount_unmountItemsOffScreen_withScroll() {
    val info_child1 = LifecycleTracker()
    val info_child2 = LifecycleTracker()
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val root =
        Column.create(legacyLithoViewRule.context)
            .child(
                MountSpecLifecycleTester.create(legacyLithoViewRule.context)
                    .intrinsicSize(Size(10, 10))
                    .lifecycleTracker(info_child1)
                    .key("some_key"))
            .child(
                MountSpecLifecycleTester.create(legacyLithoViewRule.context)
                    .intrinsicSize(Size(10, 10))
                    .lifecycleTracker(info_child2)
                    .key("other_key"))
            .child(
                SimpleStateUpdateEmulator.create(legacyLithoViewRule.context).caller(stateUpdater))
            .build()
    legacyLithoViewRule.setRoot(root).setSizePx(10, 40).attachToWindow().measure().layout()
    val parent = FrameLayout(context.androidContext)
    parent.measure(exactly(100), exactly(100))
    parent.layout(0, 0, 10, 40)
    parent.addView(legacyLithoViewRule.lithoView, 0, 40)
    val scrollView = ScrollView(context.androidContext)
    scrollView.measure(exactly(10), exactly(20))
    scrollView.layout(0, 0, 10, 20)
    scrollView.addView(parent, 10, 40)
    assertThat(info_child1.steps).describedAs("Mounted.").contains(LifecycleStep.ON_MOUNT)
    assertThat(info_child2.steps).describedAs("Mounted.").contains(LifecycleStep.ON_MOUNT)
    stateUpdater.increment()
    info_child1.reset()
    info_child2.reset()
    scrollView.scrollBy(0, 12)
    legacyLithoViewRule.dispatchGlobalLayout()
    assertThat(info_child1.steps).describedAs("Mounted.").contains(LifecycleStep.ON_UNMOUNT)
  }

  @Test
  fun incrementalMount_dirtyMount_unmountItemsOffScreen_withTranslation() {
    // When self-managing LithoViews, translation is ignored. Therefore, this test is redundant
    // when the config is enabled.
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val info_child1 = LifecycleTracker()
    val info_child2 = LifecycleTracker()
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val root =
        Column.create(legacyLithoViewRule.context)
            .child(
                MountSpecLifecycleTester.create(legacyLithoViewRule.context)
                    .intrinsicSize(Size(10, 10))
                    .lifecycleTracker(info_child1)
                    .key("some_key"))
            .child(
                MountSpecLifecycleTester.create(legacyLithoViewRule.context)
                    .intrinsicSize(Size(10, 10))
                    .lifecycleTracker(info_child2)
                    .key("other_key"))
            .child(
                SimpleStateUpdateEmulator.create(legacyLithoViewRule.context).caller(stateUpdater))
            .build()
    legacyLithoViewRule.setRoot(root).setSizePx(10, 20).attachToWindow().measure().layout()
    val parent = FrameLayout(context.androidContext)
    parent.measure(exactly(100), exactly(100))
    parent.layout(0, 0, 10, 20)
    parent.addView(legacyLithoViewRule.lithoView, 0, 20)
    assertThat(info_child1.steps).describedAs("Mounted.").contains(LifecycleStep.ON_MOUNT)
    assertThat(info_child2.steps).describedAs("Mounted.").contains(LifecycleStep.ON_MOUNT)
    stateUpdater.increment()
    info_child1.reset()
    info_child2.reset()
    legacyLithoViewRule.lithoView.translationY = -12f
    assertThat(info_child1.steps).describedAs("Mounted.").contains(LifecycleStep.ON_UNMOUNT)
  }

  @Test
  fun incrementalMount_setVisibilityHintFalse_preventMount() {
    val child1 = TestViewComponent.create(context).build()
    val child2 = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(child1, 1)
    val invisibleEventHandler = EventHandler<InvisibleEvent>(child1, 2)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(child1)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .child(
                Wrapper.create(context)
                    .delegate(child2)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(20))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    lithoView.mountComponent(Rect(0, 0, 10, 5), true)
    assertThat(child2.isMounted).isFalse
    child1.dispatchedEventHandlers.clear()
    child1.resetInteractions()
    lithoView.setVisibilityHint(false, true)
    assertThat(child1.wasOnMountCalled()).isFalse
    assertThat(child1.wasOnUnmountCalled()).isFalse
    assertThat(child1.dispatchedEventHandlers).contains(invisibleEventHandler)
    assertThat(child1.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    child1.dispatchedEventHandlers.clear()
    child1.resetInteractions()
    child2.resetInteractions()
    lithoView.mountComponent(Rect(0, 0, 10, 20), true)
    assertThat(child2.wasOnMountCalled()).isFalse
    assertThat(child1.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    assertThat(child1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler)
  }

  @Test
  fun incrementalMount_setVisibilityHintTrue_mountIfNeeded() {
    val child1 = TestViewComponent.create(context).build()
    val visibleEventHandler1 = EventHandler<VisibleEvent>(child1, 1)
    val invisibleEventHandler1 = EventHandler<InvisibleEvent>(child1, 2)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(child1)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(100))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    assertThat(child1.dispatchedEventHandlers).contains(visibleEventHandler1)
    lithoView.setVisibilityHint(false, true)
    val child2 = TestViewComponent.create(context).build()
    val visibleEventHandler2 = EventHandler<VisibleEvent>(child2, 3)
    val invisibleEventHandler2 = EventHandler<InvisibleEvent>(child2, 4)
    val newRoot =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(child1)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .widthPx(10)
                    .heightPx(10))
            .child(
                Wrapper.create(context)
                    .delegate(child2)
                    .visibleHandler(visibleEventHandler2)
                    .invisibleHandler(invisibleEventHandler2)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    lithoView.componentTree?.root = newRoot
    assertThat(child2.wasOnMountCalled()).isFalse
    assertThat(child2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    lithoView.setVisibilityHint(true, true)
    assertThat(child2.wasOnMountCalled()).isTrue
    assertThat(child2.dispatchedEventHandlers).contains(visibleEventHandler2)
  }

  @Test
  fun dirtyMount_visibleRectChanged_unmountItemNotInVisibleBounds() {
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val lifecycleTracker3 = LifecycleTracker()
    val child1 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker1).build()
    val child2 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val child3 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker3).build()
    val root1 =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child3).widthPx(10).heightPx(10))
            .build()
    val binderConfig =
        RecyclerBinderConfiguration.create().lithoViewFactory(lithoViewFactory).build()
    val config =
        ListRecyclerConfiguration.create().recyclerBinderConfiguration(binderConfig).build()
    val rcc =
        RecyclerCollectionComponent.create(context)
            .recyclerConfiguration(config)
            .section(
                SingleComponentSection.create(SectionContext(context)).component(root1).build())
            .build()
    legacyLithoViewRule
        .setRoot(rcc)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(19))
        .measure()
        .layout()
    assertThat(lifecycleTracker1.steps).contains(LifecycleStep.ON_MOUNT)
    assertThat(lifecycleTracker2.steps).contains(LifecycleStep.ON_MOUNT)
    assertThat(lifecycleTracker3.steps).doesNotContain(LifecycleStep.ON_MOUNT)
    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()
    val root2 =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(
                        MountSpecLifecycleTester.create(context)
                            .lifecycleTracker(lifecycleTracker1)
                            .build())
                    .widthPx(10)
                    .heightPx(20))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(10))
            .child(Wrapper.create(context).delegate(child3).widthPx(10).heightPx(10))
            .build()
    val rcc2 =
        RecyclerCollectionComponent.create(context)
            .recyclerConfiguration(config)
            .section(
                SingleComponentSection.create(SectionContext(context))
                    .component(root2)
                    .sticky(true)
                    .build())
            .build()
    legacyLithoViewRule.setRoot(rcc2)
    runToEndOfTasks()
    legacyLithoViewRule.dispatchGlobalLayout()
    assertThat(lifecycleTracker2.steps).contains(LifecycleStep.ON_UNMOUNT)
  }

  @Test
  fun incrementalMount_testScrollDownAndUp_correctMountUnmountCalls() {
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val lifecycleTracker3 = LifecycleTracker()
    val CHILD_HEIGHT = 10
    val child1 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker1).build()
    val child2 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val child3 =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker3).build()

    // Item is composed of 3 children of equal size (10x10), making 1 item of height 30.
    val item =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(CHILD_HEIGHT))
            .child(Wrapper.create(context).delegate(child2).widthPx(10).heightPx(CHILD_HEIGHT))
            .child(Wrapper.create(context).delegate(child3).widthPx(10).heightPx(CHILD_HEIGHT))
            .build()
    val binderConfig =
        RecyclerBinderConfiguration.create().lithoViewFactory(lithoViewFactory).build()
    val config =
        ListRecyclerConfiguration.create().recyclerBinderConfiguration(binderConfig).build()
    val sectionContext = SectionContext(context)
    val rcc =
        RecyclerCollectionComponent.create(context)
            .recyclerConfiguration(config)
            .section(
                DynamicComponentGroupSection.create(sectionContext)
                    .component(item)
                    .totalItems(5)
                    .build())
            .build()

    // Set LithoView with height so that it can fully show exactly 3 items (3 children per item).
    legacyLithoViewRule
        .setRoot(rcc)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(CHILD_HEIGHT * 9))
        .measure()
        .layout()

    // Obtain the RV for scrolling later
    val recyclerView =
        (legacyLithoViewRule.lithoView.getChildAt(0) as SectionsRecyclerView).recyclerView

    // All 3 children are visible 3 times, so we should see ON_MOUNT being called 3 times
    // for each child
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(3)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(3)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(3)

    // Clear the lifecycle steps
    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()

    // Scroll down by the size of 1 child. We are expecting to top item's child1 to be
    // unmounted, and a new bottom item's child1 to be mounted.
    recyclerView.scrollBy(0, CHILD_HEIGHT)

    // Ensure unmount is called once
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_UNMOUNT))
        .isEqualTo(1)

    // Ensure mount is called once
    // When using Litho's inc-mount, the exiting item will be mounted twice due to an issue with
    // the calculation there. Inc-mount-ext does not have this issue.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(1)

    // child2 & 3 of all items should not change.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_UNMOUNT))
        .isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_UNMOUNT))
        .isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(0)

    // Clear the lifecycle steps
    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()

    // Scroll up by the size of 1 component. We are expecting to top item's child1 to be mounted,
    // and the bottom item to be unmounted
    recyclerView.scrollBy(0, -CHILD_HEIGHT)

    // Ensure unmount is called once
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_UNMOUNT))
        .isEqualTo(1)

    // Ensure mount is called once
    // When using Litho's inc-mount, the item we previously expected to exit is still there, so
    // we don't expect a mount to occur.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(1)

    // child2 & 3 of all items should not change.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_UNMOUNT))
        .isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_UNMOUNT))
        .isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_MOUNT))
        .isEqualTo(0)
  }

  private val lithoViewFactory: LithoViewFactory
    get() = LithoViewFactory { context -> LithoView(context) }

  private class TestLithoView(context: Context?) : LithoView(context) {
    private val previousIncrementalMountBounds = Rect()

    override fun notifyVisibleBoundsChanged(visibleRect: Rect, processVisibilityOutputs: Boolean) {
      println("performIncMount on TestLithoView")
      previousIncrementalMountBounds.set(visibleRect)
    }

    override fun isIncrementalMountEnabled(): Boolean = true
  }

  companion object {
    /** Returns the amount of steps that match the given step in the given list of steps */
    private fun getCountOfLifecycleSteps(steps: List<LifecycleStep>, step: LifecycleStep): Int {
      var count = 0
      for (i in steps.indices) {
        if (steps[i] == step) {
          count++
        }
      }
      return count
    }

    private fun enableAccessibility() {
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
      val manager =
          Shadows.shadowOf(
              ApplicationProvider.getApplicationContext<Context>()
                  .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
      manager.setEnabled(true)
      manager.setTouchExplorationEnabled(true)
    }

    private fun getMockLithoViewWithBounds(bounds: Rect): LithoView {
      val lithoView: LithoView = mock()
      whenever(lithoView.left).thenReturn(bounds.left)
      whenever(lithoView.top).thenReturn(bounds.top)
      whenever(lithoView.right).thenReturn(bounds.right)
      whenever(lithoView.bottom).thenReturn(bounds.bottom)
      whenever(lithoView.width).thenReturn(bounds.width())
      whenever(lithoView.height).thenReturn(bounds.height())
      whenever(lithoView.isIncrementalMountEnabled).thenReturn(true)
      return lithoView
    }
  }
}
