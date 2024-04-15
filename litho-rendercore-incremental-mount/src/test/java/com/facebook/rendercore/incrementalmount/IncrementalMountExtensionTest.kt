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

package com.facebook.rendercore.incrementalmount

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.facebook.rendercore.HostView
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.MountState
import com.facebook.rendercore.RenderState
import com.facebook.rendercore.RenderTree
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RootHostView
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState
import com.facebook.rendercore.testing.LayoutResultWrappingNode
import com.facebook.rendercore.testing.RenderCoreTestRule
import com.facebook.rendercore.testing.SimpleLayoutResult
import com.facebook.rendercore.testing.TestHostRenderUnit
import com.facebook.rendercore.testing.TestRenderUnit
import com.facebook.rendercore.testing.ViewWrapperUnit
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19])
class IncrementalMountExtensionTest {
  @Rule @JvmField val renderCoreTestRule = RenderCoreTestRule()

  @Test
  fun whenVisibleBoundsIsEqualToHierarchy_shouldMountEverything() {
    val c = renderCoreTestRule.context
    val extensions = arrayOf<RenderCoreExtension<*, *>>(IncrementalMountRenderCoreExtension())
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(TextView(c), 1))
            .width(100)
            .height(100)
            .build()
    renderCoreTestRule
        .useExtensions(extensions)
        .useRootNode(LayoutResultWrappingNode(root))
        .setSizePx(100, 100)
        .render()
    val host = renderCoreTestRule.rootHost as HostView
    assertThat(host.childCount).isEqualTo(1)
    assertThat(host.getChildAt(0)).isInstanceOf(TextView::class.java)
  }

  @Test
  fun whenVisibleBoundsIntersectsHierarchy_shouldMountEverything() {
    val c = renderCoreTestRule.context
    val extensions = arrayOf<RenderCoreExtension<*, *>>(IncrementalMountRenderCoreExtension())
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(TextView(c), 1))
            .width(100)
            .height(100)
            .build()
    renderCoreTestRule
        .useExtensions(extensions)
        .useRootNode(LayoutResultWrappingNode(root))
        .setSizePx(50, 50)
        .render()
    val host = renderCoreTestRule.rootHost as HostView
    assertThat(host.childCount).isEqualTo(1)
    assertThat(host.getChildAt(0)).isInstanceOf(TextView::class.java)
  }

  @Test
  fun whenVisibleBoundsIsZero_shouldNotMountAnything() {
    val c = renderCoreTestRule.context
    val extensions = arrayOf<RenderCoreExtension<*, *>>(IncrementalMountRenderCoreExtension())
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(TextView(c), 1))
            .width(100)
            .height(100)
            .build()
    renderCoreTestRule
        .useExtensions(extensions)
        .useRootNode(LayoutResultWrappingNode(root))
        .setSizePx(0, 0)
        .render()
    val host = renderCoreTestRule.rootHost as HostView
    assertThat(host.childCount).isEqualTo(0)
  }

  @Test
  fun whenVisibleBoundsChangeWithBoundaryConditions_shouldMountAndUnMountCorrectly() {
    val c = renderCoreTestRule.context
    val parent = FrameLayout(c)
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY))
    parent.layout(0, 0, 10, 300)
    val host = RootHostView(c)
    parent.addView(host)
    val extensions = arrayOf<RenderCoreExtension<*, *>>(IncrementalMountRenderCoreExtension())
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .width(100)
            .height(300)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 2))
                    .y(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 3))
                    .y(200)
                    .width(100)
                    .height(100))
            .build()
    renderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(LayoutResultWrappingNode(root))
        .setSizePx(100, 300)
        .render()
    assertThat(host.childCount).isEqualTo(3)

    // Translate host up to boundary condition.
    host.offsetTopAndBottom(100)
    assertThat(host.childCount).isEqualTo(3)

    // Translate host beyond the boundary condition.
    host.offsetTopAndBottom(1) // 100 + 1 = 101
    assertThat(host.childCount).isEqualTo(2)

    // Translate host up to boundary condition is reverse direction.
    host.offsetTopAndBottom(-1 - 100 - 100) // 101 - 1 - 100 - 100 = -100
    assertThat(host.childCount).isEqualTo(2)
  }

  @Test
  fun whenVisibleBoundsChangeWithItemWindowSkipped_shouldMountAndUnMountCorrectly() {
    val c = renderCoreTestRule.context
    val parent = FrameLayout(c)
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY))
    parent.layout(0, 0, 10, 300)
    val host = RootHostView(c)
    parent.addView(host)
    val extensions = arrayOf<RenderCoreExtension<*, *>>(IncrementalMountRenderCoreExtension())
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .width(100)
            .height(300)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 2))
                    .y(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 3))
                    .y(200)
                    .width(100)
                    .height(100))
            .build()
    renderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(LayoutResultWrappingNode(root))
        .setSizePx(100, 300)
        .render()
    assertThat(host.childCount).isEqualTo(3)
    host.offsetTopAndBottom(250) // visible window skips over item id 1 and 2
    assertThat(host.childCount).describedAs("Should unmount item id 1 and 2").isEqualTo(1)
  }

  @Test
  fun whenVisibleBoundsChangeHorizontally_shouldMountAndUnMountCorrectly() {
    val c = renderCoreTestRule.context
    val parent = FrameLayout(c)
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
    parent.layout(0, 0, 300, 100)
    val host = RootHostView(c)
    parent.addView(host)
    val extensions = arrayOf<RenderCoreExtension<*, *>>(IncrementalMountRenderCoreExtension())
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .width(300)
            .height(100)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 2))
                    .x(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 3))
                    .x(200)
                    .width(100)
                    .height(100))
            .build()
    renderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(LayoutResultWrappingNode(root))
        .setSizePx(300, 100)
        .render()
    assertThat(host.childCount).isEqualTo(3)

    // Translate host up to boundary condition.
    host.offsetLeftAndRight(99)
    assertThat(host.childCount).isEqualTo(3)

    // Translate host beyond the boundary condition.
    host.offsetLeftAndRight(1) // 100 + 1 = 101
    assertThat(host.childCount).isEqualTo(2)

    // Translate host up to boundary condition is reverse direction.
    host.offsetLeftAndRight(-1 - 99 - 99) // 101 - 1 - 100 - 100 = -100
    assertThat(host.childCount).isEqualTo(3)

    // Translate host beyond the boundary condition is reverse direction.
    host.offsetLeftAndRight(-1) // -100 - 1 = -101
    assertThat(host.childCount).isEqualTo(2)
  }

  @Test
  fun whenPreviousHostIsMovedOutOfBounds_shouldMountItemsCorrectly() {
    val c = renderCoreTestRule.context
    val parent = FrameLayout(c)
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(99, View.MeasureSpec.EXACTLY))
    parent.layout(0, 0, 100, 200)
    val host = RootHostView(c)
    parent.addView(host)
    val extensions = arrayOf<RenderCoreExtension<*, *>>(IncrementalMountRenderCoreExtension())
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .width(100)
            .height(201)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(HostView(c), 1))
                    .width(100)
                    .height(100)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(ViewWrapperUnit(HostView(c), 2))
                            .width(100)
                            .height(100)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(ViewWrapperUnit(TextView(c), 3))
                                    .width(100)
                                    .height(100))))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(HostView(c), 4))
                    .y(100)
                    .width(100)
                    .height(101))
            .build()
    host.translationY = 100f
    renderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(LayoutResultWrappingNode(root))
        .setSizePx(100, 201)
        .render()
    assertThat(host.childCount).isEqualTo(2)
    assertThat((host.getChildAt(0) as HostView).childCount).isEqualTo(1)
    val newRoot: LayoutResult =
        SimpleLayoutResult.create()
            .width(100)
            .height(201)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(HostView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(HostView(c), 4))
                    .y(100)
                    .width(100)
                    .height(101)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(ViewWrapperUnit(HostView(c), 2)) // Host changed.
                            .y(1)
                            .width(100)
                            .height(100)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(ViewWrapperUnit(TextView(c), 3))
                                    .width(100)
                                    .height(100))))
            .build()
    renderCoreTestRule.useRootNode(LayoutResultWrappingNode(newRoot)).setSizePx(100, 201).render()

    // Un-mounts the Host with id 2.
    assertThat((host.getChildAt(0) as HostView).childCount).isEqualTo(0)
    // Host with id 2 is not mounted because it is outside of visible bounds.
    assertThat((host.getChildAt(1) as HostView).childCount).isEqualTo(0)

    // Scroll Host with id 2 into the view port
    host.offsetTopAndBottom(-2)

    // Host with id 2 is mounted because it enters the visible bounds.
    assertThat((host.getChildAt(1) as HostView).childCount).isEqualTo(1)
  }

  @Test
  fun whenItemBoundsAreOutsideHostBounds_shouldMountHostBeforeItem() {
    val c = renderCoreTestRule.context
    val parent = FrameLayout(c)
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
    parent.layout(0, 0, 100, 100)
    val host = RootHostView(c)
    parent.addView(host)
    val extensions = arrayOf<RenderCoreExtension<*, *>>(IncrementalMountRenderCoreExtension())
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(HostView(c), 1))
            .width(100)
            .height(100)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 3))
                    .y(110)
                    .width(100)
                    .height(100))
            .build()
    host.translationY = -105f
    renderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(LayoutResultWrappingNode(root))
        .setSizePx(100, 210)
        .render()
    assertThat(host.childCount).isEqualTo(1)
    assertThat((host.getChildAt(0) as HostView).childCount).isEqualTo(1)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun testNegativeMarginChild_forcesHostMount() {
    val c = ApplicationProvider.getApplicationContext<Context>()
    val extension = IncrementalMountExtension.getInstance()
    val mountState = createMountState(c)
    val extensionState =
        mountState.registerMountExtension(extension)
            as ExtensionState<IncrementalMountExtensionState>
    val rootHost = IncrementalMountOutput(0, 0, Rect(0, 100, 100, 200), false, null)
    val hostRenderUnit = TestHostRenderUnit(0)
    val hostRenderTreeNode = RenderTreeNode(null, hostRenderUnit, null, rootHost.bounds, null, 0)
    val host1 = IncrementalMountOutput(1, 1, Rect(0, 100, 50, 200), false, rootHost)
    val host1RenderUnit = TestHostRenderUnit(1)
    val host1RTN = RenderTreeNode(hostRenderTreeNode, host1RenderUnit, null, host1.bounds, null, 0)
    val child1 = IncrementalMountOutput(2, 2, Rect(0, 85, 50, 190), false, host1)
    val child1RenderUnit = TestRenderUnit(2)
    val child11RTN = RenderTreeNode(host1RTN, child1RenderUnit, null, child1.bounds, null, 0)
    val host2 = IncrementalMountOutput(3, 3, Rect(50, 100, 50, 200), false, rootHost)
    val host2RenderUnit = TestHostRenderUnit(3)
    val host2RTN = RenderTreeNode(hostRenderTreeNode, host2RenderUnit, null, host2.bounds, null, 1)
    val child2 = IncrementalMountOutput(4, 4, Rect(50, 100, 50, 200), false, host2)
    val child2RenderUnit = TestRenderUnit(4)
    val child2RTN = RenderTreeNode(host2RTN, child2RenderUnit, null, child2.bounds, null, 0)
    val flatList = arrayOf(hostRenderTreeNode, host1RTN, child11RTN, host2RTN, child2RTN)
    val renderTree =
        RenderTree(
            hostRenderTreeNode,
            flatList,
            null,
            SizeConstraints.exact(100, 100),
            RenderState.NO_ID,
            null,
            null)
    val input1 = TestIncrementalMountExtensionInput(rootHost, host1, child1, host2, child2)
    extension.beforeMount(extensionState, input1, Rect(0, 80, 100, 90))
    mountState.mount(renderTree)
    assertThat(extensionState.ownsReference(1)).isTrue
    assertThat(extensionState.ownsReference(2)).isTrue
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun testNegativeMarginChild_hostMovedAndUnmounted_forcesHostMount() {
    val c = ApplicationProvider.getApplicationContext<Context>()
    val extension = IncrementalMountExtension.getInstance()
    val mountState = createMountState(c)
    val extensionState =
        mountState.registerMountExtension(extension)
            as ExtensionState<IncrementalMountExtensionState>
    val rootHost = IncrementalMountOutput(0, 0, Rect(0, 100, 100, 200), false, null)
    val hostRenderUnit = TestHostRenderUnit(0)
    val hostRenderTreeNode = RenderTreeNode(null, hostRenderUnit, null, rootHost.bounds, null, 0)
    val host1 = IncrementalMountOutput(1, 1, Rect(0, 100, 50, 200), false, rootHost)
    val host1RenderUnit = TestHostRenderUnit(1)
    val host1RTN = RenderTreeNode(hostRenderTreeNode, host1RenderUnit, null, host1.bounds, null, 0)
    val child1 = IncrementalMountOutput(2, 2, Rect(0, 85, 50, 190), false, host1)
    val child1RenderUnit = TestRenderUnit(2)
    val child11RTN = RenderTreeNode(host1RTN, child1RenderUnit, null, child1.bounds, null, 0)
    val host2 = IncrementalMountOutput(3, 3, Rect(50, 100, 50, 200), false, rootHost)
    val host2RenderUnit = TestHostRenderUnit(3)
    val host2RTN = RenderTreeNode(hostRenderTreeNode, host2RenderUnit, null, host2.bounds, null, 1)
    val child2 = IncrementalMountOutput(4, 4, Rect(50, 100, 50, 200), false, host2)
    val child2RenderUnit = TestRenderUnit(4)
    val child2RTN = RenderTreeNode(host2RTN, child2RenderUnit, null, child2.bounds, null, 0)
    val flatList = arrayOf(hostRenderTreeNode, host2RTN, child2RTN, host1RTN, child11RTN)
    val renderTree =
        RenderTree(
            hostRenderTreeNode,
            flatList,
            null,
            SizeConstraints.exact(100, 100),
            RenderState.NO_ID,
            null,
            null)
    val input = TestIncrementalMountExtensionInput(rootHost, host2, child2, host1, child1)
    extension.beforeMount(extensionState, input, Rect(0, 80, 100, 90))
    mountState.mount(renderTree)
    assertThat(extensionState.ownsReference(1)).isTrue
    assertThat(extensionState.ownsReference(2)).isTrue
    val host1Reparented = IncrementalMountOutput(1, 1, Rect(0, 100, 50, 200), false, host2)
    val host1RenderUnitReparented = TestHostRenderUnit(1)
    val host1RTNReparented =
        RenderTreeNode(host2RTN, host1RenderUnitReparented, null, host1Reparented.bounds, null, 1)
    val child1Reparented =
        IncrementalMountOutput(2, 2, Rect(0, 85, 50, 190), false, host1Reparented)
    val child1RenderUnitReparented = TestRenderUnit(2)
    val child11RTNReparented =
        RenderTreeNode(
            host1RTNReparented, child1RenderUnitReparented, null, child1Reparented.bounds, null, 0)
    val flatListReparented =
        arrayOf(hostRenderTreeNode, host2RTN, child2RTN, host1RTNReparented, child11RTNReparented)
    val renderTreeReparented =
        RenderTree(
            hostRenderTreeNode,
            flatListReparented,
            null,
            SizeConstraints.exact(100, 100),
            RenderState.NO_ID,
            null,
            null)
    val inputReparented =
        TestIncrementalMountExtensionInput(
            rootHost, host2, child2, host1Reparented, child1Reparented)
    extension.beforeMount(extensionState, inputReparented, Rect(0, 80, 100, 90))
    mountState.mount(renderTreeReparented)
    assertThat(extensionState.ownsReference(1)).isTrue
    assertThat(extensionState.ownsReference(2)).isTrue
  }

  companion object {
    private fun createMountState(c: Context): MountState {
      return MountState(RootHostView(c))
    }
  }
}
