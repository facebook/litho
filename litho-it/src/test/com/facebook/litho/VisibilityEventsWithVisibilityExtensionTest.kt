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

import android.widget.FrameLayout
import com.facebook.litho.Column.Companion.create
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import com.facebook.rendercore.Reducer
import com.facebook.rendercore.RenderTree
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.visibility.VisibilityMountExtension
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class VisibilityEventsWithVisibilityExtensionTest {

  @JvmField @Rule val lithoViewRule = LithoTestRule()

  @Test
  fun visibilityExtensionOnUnmountAllItems_shouldUnmount() {
    val context = lithoViewRule.context
    val content: SpecGeneratedComponent = Text.create(context).text("hello world").build()
    val visibleEventHandler = EventHandlerTestUtil.create<VisibleEvent>(2, content)
    val testLithoView = lithoViewRule.createTestLithoView()
    FrameLayout(context.androidContext).apply {
      left = 0
      top = 0
      right = 10
      bottom = 10
      addView(testLithoView.lithoView)
    }
    lithoViewRule.render(lithoView = testLithoView.lithoView) {
      create(context)
          .child(Wrapper.create(context).delegate(content).visibleHandler(visibleEventHandler))
          .build()
    }
    val layoutState: LayoutState = mock()
    val renderTree: RenderTree = mock()
    val rootNode: RenderTreeNode = mock()
    whenever(layoutState.toRenderTree()).thenReturn(renderTree)
    whenever(renderTree.getRenderTreeNodeAtIndex(0)).thenReturn(rootNode)
    whenever(rootNode.renderUnit).thenReturn(Reducer.ROOT_HOST_RENDER_UNIT)
    testLithoView.lithoView.setMountStateDirty()
    val visibilityExtension: VisibilityMountExtension<*> =
        spy(VisibilityMountExtension.getInstance())
    useVisibilityOutputsExtension(testLithoView.lithoView, visibilityExtension)
    testLithoView.lithoView.unmountAllItems()
    verify(visibilityExtension).onUnbind(any())
    verify(visibilityExtension).onUnmount(any())
  }

  private fun useVisibilityOutputsExtension(
      lithoView: LithoView,
      visibilityOutputsExtension: VisibilityMountExtension<*>
  ) {
    val lithoHostListenerCoordinator =
        Whitebox.getInternalState<LithoHostListenerCoordinator>(
            lithoView, "lithoHostListenerCoordinator")
    lithoHostListenerCoordinator.useVisibilityExtension(visibilityOutputsExtension)
  }
}
