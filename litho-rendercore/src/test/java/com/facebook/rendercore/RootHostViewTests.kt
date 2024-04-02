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
import android.view.View
import com.facebook.rendercore.RenderUnit.DelegateBinder.Companion.createDelegateBinder
import com.facebook.rendercore.testing.RenderCoreTestRule.IdentityResolveFunc
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.testing.TestRenderUnit
import org.assertj.core.api.Java6Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RootHostViewTests {

  @Test
  fun testMountOnLayout() {
    val c: Context = RuntimeEnvironment.getApplication()
    val node = TestNode()
    val renderUnit: RenderUnit<*> = TestRenderUnit()
    val didMount = BooleanArray(1)
    renderUnit.addOptionalMountBinder(
        createDelegateBinder(null, SimpleTestBinder { didMount[0] = true }))
    node.setRenderUnit(renderUnit)
    val rootHostView = RootHostView(c)
    val renderState =
        RenderState<Any?, Any?, StateUpdateReceiver.StateUpdate<Any?>>(
            c,
            object : RenderState.Delegate<Any?> {
              override fun commit(
                  layoutVersion: Int,
                  current: RenderTree?,
                  next: RenderTree,
                  currentState: Any?,
                  nextState: Any?
              ) = Unit

              override fun commitToUI(tree: RenderTree?, state: Any?, frameId: Int) = Unit
            },
            null,
            null)
    rootHostView.setRenderState(renderState)
    renderState.setTree(
        IdentityResolveFunc(node)
            as RenderState.ResolveFunc<Any?, Any?, StateUpdateReceiver.StateUpdate<Any?>>)
    rootHostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST))
    Java6Assertions.assertThat(didMount[0]).isFalse
    rootHostView.layout(0, 0, rootHostView.measuredWidth, rootHostView.measuredHeight)
    Java6Assertions.assertThat(didMount[0]).isTrue
  }

  @Test
  fun testNestedMount() {
    val c: Context = RuntimeEnvironment.getApplication()
    val renderState =
        RenderState<Any?, Any?, StateUpdateReceiver.StateUpdate<Any?>>(
            c,
            object : RenderState.Delegate<Any?> {
              override fun commit(
                  layoutVersion: Int,
                  current: RenderTree?,
                  next: RenderTree,
                  currentState: Any?,
                  nextState: Any?
              ) = Unit

              override fun commitToUI(tree: RenderTree?, state: Any?, frameId: Int) = Unit
            },
            null,
            null)
    val didMount = BooleanArray(1)
    val nestedMountTestNode = TestNode()
    val nestedMountRenderUnit: RenderUnit<*> = TestRenderUnit()
    nestedMountRenderUnit.addOptionalMountBinder(
        createDelegateBinder(null, SimpleTestBinder { didMount[0] = true }))
    nestedMountTestNode.setRenderUnit(nestedMountRenderUnit)
    val node = TestNode()
    val renderUnit: RenderUnit<*> = TestRenderUnit()
    renderUnit.addOptionalMountBinder(
        createDelegateBinder(
            null,
            SimpleTestBinder {
              renderState.setTree(
                  IdentityResolveFunc(nestedMountTestNode)
                      as RenderState.ResolveFunc<Any?, Any?, StateUpdateReceiver.StateUpdate<Any?>>)
            }))
    node.setRenderUnit(renderUnit)
    val rootHostView = RootHostView(c)
    rootHostView.setRenderState(renderState)
    renderState.setTree(
        IdentityResolveFunc(node)
            as RenderState.ResolveFunc<Any?, Any?, StateUpdateReceiver.StateUpdate<Any?>>)
    rootHostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST))
    Java6Assertions.assertThat(didMount[0]).isFalse
    rootHostView.layout(0, 0, rootHostView.measuredWidth, rootHostView.measuredHeight)
    Java6Assertions.assertThat(didMount[0]).isTrue
  }
}
