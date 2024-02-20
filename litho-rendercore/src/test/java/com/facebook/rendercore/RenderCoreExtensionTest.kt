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

package com.facebook.rendercore

import android.graphics.Rect
import android.view.View
import android.widget.TextView
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.InformsMountCallback
import com.facebook.rendercore.extensions.LayoutResultVisitor
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.OnItemCallbacks
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.extensions.VisibleBoundsCallbacks
import com.facebook.rendercore.testing.LayoutResultWrappingNode
import com.facebook.rendercore.testing.RenderCoreTestRule
import com.facebook.rendercore.testing.SimpleLayoutResult
import com.facebook.rendercore.testing.TestRenderCoreExtension
import com.facebook.rendercore.testing.ViewAssertions
import com.facebook.rendercore.testing.ViewWrapperUnit
import com.facebook.rendercore.testing.match.ViewMatchNode
import org.assertj.core.api.Java6Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RenderCoreExtensionTest {

  @Rule @JvmField val renderCoreTestRule = RenderCoreTestRule()

  @Test
  fun onRenderWithExtension_shouldRenderView() {
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(TextView(renderCoreTestRule.context), 1))
            .width(100)
            .height(100)
            .build()
    val visitor = TrackingLayoutResultVisitor()
    val extension = TrackingMountExtension()
    val e1 = RenderCoreExtension<Any?, Any?>()
    val e2 = TestRenderCoreExtension()
    val e3 = TestRenderCoreExtension(visitor) { ArrayList<Any>() }
    val e4 = TestRenderCoreExtension(extension)
    renderCoreTestRule
        .useExtensions(arrayOf(e1, e2, e3, e4))
        .useRootNode(LayoutResultWrappingNode(root))
        .render()
    val rootView = renderCoreTestRule.rootHost as View
    ViewAssertions.assertThat(rootView)
        .matches(
            ViewMatchNode.forType(Host::class.java)
                .child(ViewMatchNode.forType(TextView::class.java).bounds(0, 0, 100, 100)))
    Java6Assertions.assertThat(visitor.count).isEqualTo(2)
    Java6Assertions.assertThat(extension.beforeMount).isEqualTo(1)
    Java6Assertions.assertThat(extension.afterMount).isEqualTo(1)
    Java6Assertions.assertThat(extension.onVisibleBoundsChanged).isEqualTo(0)
    rootView.offsetLeftAndRight(100)
    Java6Assertions.assertThat(extension.onVisibleBoundsChanged).isEqualTo(1)
    rootView.offsetTopAndBottom(100)
    Java6Assertions.assertThat(extension.onVisibleBoundsChanged).isEqualTo(2)
  }

  @Test
  fun onRenderWithNewExtensions_shouldRenderViewAndDiscardOldExtensions() {
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(TextView(renderCoreTestRule.context), 1))
            .width(100)
            .height(100)
            .build()
    val v1 = TrackingLayoutResultVisitor()
    val me1 = TrackingMountExtension()
    val e1 = TestRenderCoreExtension(v1) { ArrayList<Any>() }
    val e2 = TestRenderCoreExtension(me1)
    renderCoreTestRule
        .useExtensions(arrayOf(e1, e2))
        .useRootNode(LayoutResultWrappingNode(root))
        .render()
    ViewAssertions.assertThat(renderCoreTestRule.rootHost as View)
        .matches(
            ViewMatchNode.forType(Host::class.java)
                .child(ViewMatchNode.forType(TextView::class.java).bounds(0, 0, 100, 100)))
    Java6Assertions.assertThat(v1.count).isEqualTo(2)
    Java6Assertions.assertThat(me1.beforeMount).isEqualTo(1)
    Java6Assertions.assertThat(me1.afterMount).isEqualTo(1)
    Java6Assertions.assertThat(me1.onVisibleBoundsChanged).isEqualTo(0)

    // New Extensions
    val v2 = TrackingLayoutResultVisitor()
    val me2 = TrackingMountExtension()
    val e3 = TestRenderCoreExtension(v2) { ArrayList<Any>() }
    val e4 = TestRenderCoreExtension(me2)

    // Next render
    renderCoreTestRule
        .useExtensions(arrayOf(e3, e4))
        .useRootNode(LayoutResultWrappingNode(root))
        .render()
    ViewAssertions.assertThat(renderCoreTestRule.rootHost as View)
        .matches(
            ViewMatchNode.forType(Host::class.java)
                .child(ViewMatchNode.forType(TextView::class.java).bounds(0, 0, 100, 100)))

    // No interactions with the previous extensions.
    Java6Assertions.assertThat(v1.count).isEqualTo(2)
    Java6Assertions.assertThat(me1.beforeMount).isEqualTo(1)
    Java6Assertions.assertThat(me1.afterMount).isEqualTo(1)
    Java6Assertions.assertThat(me1.onVisibleBoundsChanged).isEqualTo(0)

    // Only interactions with the new extensions.
    Java6Assertions.assertThat(v2.count).isEqualTo(2)
    Java6Assertions.assertThat(me2.beforeMount).isEqualTo(1)
    Java6Assertions.assertThat(me2.afterMount).isEqualTo(1)
    Java6Assertions.assertThat(me2.onVisibleBoundsChanged).isEqualTo(0)
  }

  @Test
  fun onUnmountAllItemsWithExtensions_shouldCallbackAllExtensions() {
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(TextView(renderCoreTestRule.context), 1))
            .width(100)
            .height(100)
            .build()
    val extension = TrackingMountExtension()
    renderCoreTestRule
        .useExtensions(arrayOf<RenderCoreExtension<*, *>>(TestRenderCoreExtension(extension)))
        .useRootNode(LayoutResultWrappingNode(root))
        .render()

    // should call
    Java6Assertions.assertThat(extension.beforeMount).isEqualTo(1)
    Java6Assertions.assertThat(extension.afterMount).isEqualTo(1)
    Java6Assertions.assertThat(extension.mountItem).isEqualTo(2)
    Java6Assertions.assertThat(extension.bindItem).isEqualTo(2)

    // should not call
    Java6Assertions.assertThat(extension.unmountItem).isEqualTo(0)
    Java6Assertions.assertThat(extension.unbindItem).isEqualTo(0)
    Java6Assertions.assertThat(extension.unmount).isEqualTo(0)
    Java6Assertions.assertThat(extension.unbind).isEqualTo(0)
    Java6Assertions.assertThat(extension.onVisibleBoundsChanged).isEqualTo(0)
    renderCoreTestRule.rootHost.setRenderState(null)

    // should call
    Java6Assertions.assertThat(extension.unmountItem).isEqualTo(2)
    Java6Assertions.assertThat(extension.unbindItem).isEqualTo(2)
    Java6Assertions.assertThat(extension.unmount).isEqualTo(1)
    Java6Assertions.assertThat(extension.unbind).isEqualTo(1)
  }

  @Test
  fun onUnmountRootItem_shouldCallbackAllExtensions() {
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(TextView(renderCoreTestRule.context), 1))
            .width(100)
            .height(100)
            .build()
    val extension = TrackingMountExtension()
    renderCoreTestRule
        .useExtensions(arrayOf<RenderCoreExtension<*, *>>(TestRenderCoreExtension(extension)))
        .useRootNode(LayoutResultWrappingNode(root))
        .render()
    val rootView = renderCoreTestRule.rootHost as RootHostView
    extension.idsMarkedForRelease = longArrayOf(Reducer.ROOT_HOST_RENDER_UNIT.id)
    rootView.offsetTopAndBottom(100)
    Java6Assertions.assertThat(extension.onVisibleBoundsChanged).isEqualTo(1)
    Java6Assertions.assertThat(extension.unmountItem).isEqualTo(2)
    Java6Assertions.assertThat(extension.unbindItem).isEqualTo(2)
  }

  class TrackingLayoutResultVisitor : LayoutResultVisitor<Any?> {
    var count = 0

    override fun visit(
        parent: RenderTreeNode?,
        result: LayoutResult,
        bounds: Rect,
        x: Int,
        y: Int,
        position: Int,
        state: Any?
    ) {
      count++
    }
  }

  class TrackingMountExtension :
      MountExtension<Any?, Any?>(),
      VisibleBoundsCallbacks<Any?>,
      OnItemCallbacks<Any?>,
      InformsMountCallback {

    var beforeMount = 0
    var afterMount = 0
    var onVisibleBoundsChanged = 0
    var unmount = 0
    var unbind = 0
    var mountItem = 0
    var bindItem = 0
    var unmountItem = 0
    var unbindItem = 0
    var idsMarkedForRelease: LongArray? = null

    override fun canPreventMount(): Boolean {
      return true
    }

    override fun createState(): Any {
      return Any()
    }

    override fun beforeMount(
        extensionState: ExtensionState<Any?>,
        input: Any?,
        localVisibleRect: Rect?
    ) {
      beforeMount++
    }

    override fun afterMount(extensionState: ExtensionState<Any?>) {
      afterMount++
    }

    override fun onVisibleBoundsChanged(
        extensionState: ExtensionState<Any?>,
        localVisibleRect: Rect?
    ) {
      onVisibleBoundsChanged++
      val idsMarkedForRelease = this.idsMarkedForRelease ?: return
      for (id in idsMarkedForRelease) {

        // force acquire (without mounting) if not already acquired.
        if (!extensionState.ownsReference(id)) {
          extensionState.acquireMountReference(id, false)
        }
        extensionState.releaseMountReference(id, true)
      }
    }

    override fun beforeMountItem(
        extensionState: ExtensionState<Any?>,
        renderTreeNode: RenderTreeNode,
        index: Int
    ) {
      // mount all
      if (!extensionState.ownsReference(renderTreeNode.renderUnit.id)) {
        extensionState.acquireMountReference(renderTreeNode.renderUnit.id, false)
      }
    }

    override fun onUnmount(extensionState: ExtensionState<Any?>) {
      unmount++
    }

    override fun onUnbind(extensionState: ExtensionState<Any?>) {
      unbind++
    }

    override fun onBindItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      bindItem++
    }

    override fun onUnbindItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      unbindItem++
    }

    override fun onUnmountItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      unmountItem++
    }

    override fun onMountItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      mountItem++
    }

    override fun shouldUpdateItem(
        extensionState: ExtensionState<Any?>,
        previousRenderUnit: RenderUnit<*>,
        previousLayoutData: Any?,
        nextRenderUnit: RenderUnit<*>,
        nextLayoutData: Any?
    ): Boolean {
      return false
    }

    override fun onBoundsAppliedToItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?,
        changed: Boolean
    ) = Unit
  }
}
