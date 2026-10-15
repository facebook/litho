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

import android.content.Context
import android.graphics.Rect
import android.view.View
import com.facebook.rendercore.RenderResult.Companion.render
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.testing.TestRenderUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RenderTreeHostViewTests {

  @Test
  fun testMountOnLayout() {
    val c: Context = RuntimeEnvironment.application
    val renderTreeHostView = RenderTreeHostView(c)
    val node = TestNode()
    val renderUnit: RenderUnit<*> = TestRenderUnit()
    val didMount = BooleanArray(1)
    renderUnit.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null,
            SimpleTestBinder { didMount[0] = true },
        ),
    )
    node.setRenderUnit(renderUnit)
    val mountInput = render(
        c,
        ResolveResult<Node<Any?>, Any?>(node),
        null,
        null,
        null,
        0,
        SizeConstraints(0, 100, 0, 100),
    )
        .mountInput
    renderTreeHostView.setMountInput(mountInput)
    renderTreeHostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    renderTreeHostView.layout(
        0,
        0,
        renderTreeHostView.measuredWidth,
        renderTreeHostView.measuredHeight,
    )
    assertThat(didMount[0]).isTrue
  }

  @Test
  fun testDuplicateRenderUnitIdsInTreeCauseException() {
    val renderUnit1 = TestRenderUnit()
    val renderUnit2 = TestRenderUnit()

    // Setup tree with 2 render-units with the same ID.
    renderUnit1.setId(1)
    renderUnit2.setId(1)
    val node1 = RenderTreeNode(null, renderUnit1, null, Rect(), null, 0)
    val node2 = RenderTreeNode(null, renderUnit2, null, Rect(), null, 1)
    var exceptionOccurred = false
    var renderTree: RenderTree? = null
    try {
      // RenderTree ctor should detect duplicate RU ids but now logs an error instead of throwing.
      renderTree =
          RenderTree(
              node1,
              arrayOf(node1, node2),
              null,
              SizeConstraints.exact(0, 0),
              RenderState.NO_ID,
              null,
              null,
              null,
          )
    } catch (e: IllegalStateException) {
      // Exception should NOT occur with the new graceful error handling.
      exceptionOccurred = true
    }
    if (BuildConfig.DEBUG) {
      assertThat(exceptionOccurred).isTrue
    } else {
      // Verify that no exception was thrown and RenderTree was created successfully.
      assertThat(exceptionOccurred).isFalse
      assertThat(renderTree).isNotNull
      // Verify that only the first occurrence is kept (duplicate is skipped).
      assertThat(renderTree?.getRenderTreeNodeIndex(1)).isEqualTo(0)
    }
  }

  @Test
  fun testMeasureReturnsRenderTreeSize() {
    val c: Context = RuntimeEnvironment.application
    val renderTreeHostView = RenderTreeHostView(c)
    val node = TestNode(0, 0, 99, 99)
    val renderUnit: RenderUnit<*> = TestRenderUnit()
    node.setRenderUnit(renderUnit)
    val renderTree = render(
        c,
        ResolveResult<Node<Any?>, Any?>(node),
        null,
        null,
        null,
        0,
        SizeConstraints(0, 100, 0, 100),
    )
        .mountInput
    renderTreeHostView.setMountInput(renderTree)
    renderTreeHostView.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
    )
    assertThat(renderTreeHostView.measuredWidth).isEqualTo(99)
    assertThat(renderTreeHostView.measuredHeight).isEqualTo(99)
  }

  @Test
  fun testNestedMount() {
    val c: Context = RuntimeEnvironment.application
    val renderTreeHostView = RenderTreeHostView(c)
    val didMount = BooleanArray(1)
    val nestedMountTestNode = TestNode()
    val nestedMountRenderUnit: RenderUnit<*> = TestRenderUnit()
    nestedMountRenderUnit.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null,
            SimpleTestBinder { didMount[0] = true },
        ),
    )
    nestedMountTestNode.setRenderUnit(nestedMountRenderUnit)
    val node = TestNode()
    val renderUnit: RenderUnit<*> = TestRenderUnit()
    renderUnit.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null,
            SimpleTestBinder {
              renderTreeHostView.setMountInput(
                  render(
                      c,
                      ResolveResult<Node<Any?>, Any?>(nestedMountTestNode),
                      null,
                      null,
                      null,
                      0,
                      SizeConstraints(0, 100, 0, 100),
                  )
                      .mountInput,
              )
            },
        ),
    )
    node.setRenderUnit(renderUnit)
    val mountInput = render(
        c,
        ResolveResult<Node<Any?>, Any?>(nestedMountTestNode),
        null,
        null,
        null,
        0,
        SizeConstraints(0, 100, 0, 100),
    )
        .mountInput
    renderTreeHostView.setMountInput(mountInput)
    renderTreeHostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    assertThat(didMount[0]).isFalse
    renderTreeHostView.layout(
        0,
        0,
        renderTreeHostView.measuredWidth,
        renderTreeHostView.measuredHeight,
    )
    assertThat(didMount[0]).isTrue
  }
}
