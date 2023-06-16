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

import android.graphics.Rect
import android.widget.FrameLayout
import com.facebook.litho.Column.Companion.create
import com.facebook.litho.ComponentsSystrace.systrace
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestComponent
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import com.facebook.rendercore.Reducer
import com.facebook.rendercore.RenderTree
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.visibility.VisibilityMountExtension
import com.facebook.yoga.YogaEdge
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class VisibilityEventsWithVisibilityExtensionTest {

  private val right = 10

  private lateinit var context: ComponentContext
  private lateinit var lithoView: LithoView
  private lateinit var parent: FrameLayout

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    context = legacyLithoViewRule.context
    lithoView = LithoView(context)
    legacyLithoViewRule.useLithoView(lithoView)
    parent =
        FrameLayout(context.androidContext).apply {
          left = 0
          top = 0
          right = 10
          bottom = 10
          addView(lithoView)
        }
  }

  @Test
  fun visibilityExtensionInput_dirtyMountWithoutVisibilityProcessing_setInput() {
    val content: TestComponent = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val root: Component =
        create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(5))
        .measure()
    val layoutState: LayoutState = mock()
    val renderTree: RenderTree = mock()
    val rootNode: RenderTreeNode = mock()
    whenever(layoutState.toRenderTree()).thenReturn(renderTree)
    whenever(layoutState.tracer).thenReturn(systrace)
    whenever(renderTree.getRenderTreeNodeAtIndex(0)).thenReturn(rootNode)
    whenever(rootNode.renderUnit).thenReturn(Reducer.sRootHostRenderUnit)
    legacyLithoViewRule.lithoView.setMountStateDirty()
    val visibilityExtension: VisibilityMountExtension<LayoutState> =
        spy(VisibilityMountExtension.getInstance() as VisibilityMountExtension<LayoutState>)
    useVisibilityOutputsExtension(legacyLithoViewRule.lithoView, visibilityExtension)
    val rect = Rect(0, 0, right, 10)
    legacyLithoViewRule.lithoView.mount(layoutState, Rect(0, 0, right, 10), false)
    verify(visibilityExtension).beforeMount(any(), eq(layoutState), eq(rect))
  }

  @Test
  fun visibilityExtensionOnUnmountAllItems_shouldUnmount() {
    val content: SpecGeneratedComponent = Text.create(context).text("hello world").build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val root: Component =
        create(context)
            .child(Wrapper.create(context).delegate(content).visibleHandler(visibleEventHandler))
            .build()
    legacyLithoViewRule.setRoot(root).attachToWindow().measure().layout()
    val layoutState: LayoutState = mock()
    val renderTree: RenderTree = mock()
    val rootNode: RenderTreeNode = mock()
    whenever(layoutState.toRenderTree()).thenReturn(renderTree)
    whenever(renderTree.getRenderTreeNodeAtIndex(0)).thenReturn(rootNode)
    whenever(rootNode.renderUnit).thenReturn(Reducer.sRootHostRenderUnit)
    legacyLithoViewRule.lithoView.setMountStateDirty()
    val visibilityExtension: VisibilityMountExtension<*> =
        spy(VisibilityMountExtension.getInstance())
    useVisibilityOutputsExtension(legacyLithoViewRule.lithoView, visibilityExtension)
    legacyLithoViewRule.lithoView.unmountAllItems()
    verify(visibilityExtension).onUnbind(any())
    verify(visibilityExtension).onUnmount(any())
  }

  private fun useVisibilityOutputsExtension(
      lithoView: LithoView,
      visibilityOutputsExtension: VisibilityMountExtension<*>
  ) {
    val lithoHostListenerCoordinator =
        Whitebox.getInternalState<LithoHostListenerCoordinator>(
            lithoView, "mLithoHostListenerCoordinator")
    lithoHostListenerCoordinator.useVisibilityExtension(visibilityOutputsExtension)
  }
}
