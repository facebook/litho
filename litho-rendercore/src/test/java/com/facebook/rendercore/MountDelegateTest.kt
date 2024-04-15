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

import android.view.View
import android.widget.TextView
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.InformsMountCallback
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.OnItemCallbacks
import com.facebook.rendercore.extensions.RenderCoreExtension
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
class MountDelegateTest {

  @Rule @JvmField val renderCoreTestRule = RenderCoreTestRule()

  @Test
  fun testMountDelegateAPIs() {
    val c = renderCoreTestRule.context
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .width(200)
            .height(300)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 2))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 3))
                    .y(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 4))
                    .y(200)
                    .width(100)
                    .height(100))
            .build()
    val mountExtension = UnitIdBasedMountExtension(2, 3)
    val extension: RenderCoreExtension<*, *> = TestRenderCoreExtension(mountExtension)
    renderCoreTestRule
        .useExtensions(arrayOf(extension))
        .useRootNode(LayoutResultWrappingNode(root))
        .render()
    ViewAssertions.assertThat(renderCoreTestRule.rootHost as View)
        .matches(
            ViewMatchNode.forType(Host::class.java)
                .child(
                    ViewMatchNode.forType(TextView::class.java)
                        .bounds(0, 0, 100, 100)
                        .absoluteBoundsForRootType(0, 0, 100, 100, RootHost::class.java))
                .child(
                    ViewMatchNode.forType(TextView::class.java)
                        .bounds(0, 100, 100, 100)
                        .absoluteBoundsForRootType(0, 100, 100, 100, RootHost::class.java)))
    Java6Assertions.assertThat(mountExtension.beforeMountItem).isEqualTo(3)
    Java6Assertions.assertThat(mountExtension.onMountItem).isEqualTo(2 + 1 /* for the root*/)
    Java6Assertions.assertThat(mountExtension.onBindItem).isEqualTo(2 + 1 /* for the root*/)
    renderCoreTestRule
        .useRootNode(LayoutResultWrappingNode(SimpleLayoutResult.create().build()))
        .render()

    // No new calls to the 'before' APIs
    Java6Assertions.assertThat(mountExtension.beforeMountItem).isEqualTo(3)
    Java6Assertions.assertThat(mountExtension.onMountItem).isEqualTo(2 + 1 /* for the root*/)
    Java6Assertions.assertThat(mountExtension.onBindItem).isEqualTo(2 + 1 /* for the root*/)
    Java6Assertions.assertThat(mountExtension.onUnmountItem).isEqualTo(2)
    Java6Assertions.assertThat(mountExtension.onUnbindItem).isEqualTo(2)
  }

  internal class UnitIdBasedMountExtension(vararg ids: Long) :
      MountExtension<Any?, Map<Long, Boolean>?>(),
      OnItemCallbacks<Map<Long?, Boolean?>?>,
      InformsMountCallback {
    private val map: MutableMap<Long, Boolean>
    var beforeMountItem = 0
    var onUnmountItem = 0
    var onMountItem = 0
    var onBindItem = 0
    var onUnbindItem = 0

    init {
      map = HashMap(ids.size)
      for (id in ids) {
        map[id] = true
      }
    }

    override fun canPreventMount(): Boolean {
      return true
    }

    override fun createState(): Map<Long, Boolean> {
      return map
    }

    override fun beforeMountItem(
        extensionState: ExtensionState<Map<Long?, Boolean?>?>,
        renderTreeNode: RenderTreeNode,
        index: Int
    ) {
      beforeMountItem++
      val id = renderTreeNode.renderUnit.id
      if (extensionState.state?.containsKey(id) == true && extensionState.state?.get(id) == true) {
        extensionState.acquireMountReference(id, false)
      }
    }

    override fun onMountItem(
        extensionState: ExtensionState<Map<Long?, Boolean?>?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      onMountItem++
    }

    override fun onUnmountItem(
        extensionState: ExtensionState<Map<Long?, Boolean?>?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      onUnmountItem++
    }

    override fun onBindItem(
        extensionState: ExtensionState<Map<Long?, Boolean?>?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      onBindItem++
    }

    override fun onUnbindItem(
        extensionState: ExtensionState<Map<Long?, Boolean?>?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      onUnbindItem++
    }

    override fun shouldUpdateItem(
        extensionState: ExtensionState<Map<Long?, Boolean?>?>,
        previousRenderUnit: RenderUnit<*>,
        previousLayoutData: Any?,
        nextRenderUnit: RenderUnit<*>,
        nextLayoutData: Any?
    ): Boolean {
      return false
    }

    override fun onBoundsAppliedToItem(
        extensionState: ExtensionState<Map<Long?, Boolean?>?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?,
        changed: Boolean
    ) = Unit
  }
}
