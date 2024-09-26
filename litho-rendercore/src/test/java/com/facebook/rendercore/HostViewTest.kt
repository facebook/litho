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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.facebook.rendercore.RenderUnit.RenderType
import com.facebook.rendercore.testing.DrawableWrapperUnit
import com.facebook.rendercore.testing.LayoutResultWrappingNode
import com.facebook.rendercore.testing.RenderCoreTestRule
import com.facebook.rendercore.testing.SimpleLayoutResult
import com.facebook.rendercore.testing.ViewWrapperUnit
import org.assertj.core.api.Java6Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostViewTest {

  @Rule @JvmField val renderCoreTestRule = RenderCoreTestRule()

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun testOnInterceptTouchEvent_withHandler() {
    val hostView = HostView(context)
    val ev = MotionEvent.obtain(0, 0, 0, 0f, 0f, 0)
    Java6Assertions.assertThat(hostView.onInterceptTouchEvent(ev)).isFalse
    hostView.setInterceptTouchEventHandler(
        object : InterceptTouchHandler {
          override fun onInterceptTouchEvent(view: View, ev: MotionEvent): Boolean {
            return true
          }
        })
    Java6Assertions.assertThat(hostView.onInterceptTouchEvent(ev)).isTrue
  }

  @Test
  fun testMountViewItem() {
    val hostView = HostView(context)
    val content = View(context)
    val mountItem = createMountItem(hostView, content, 1)
    hostView.mount(0, mountItem)
    Java6Assertions.assertThat(hostView.getMountItemAt(0)).isSameAs(mountItem)
    Java6Assertions.assertThat(hostView.getChildAt(0)).isSameAs(content)
  }

  @Test
  fun testMoveItemToEnd() {
    val hostView = HostView(context)
    val mountItem = createMountItem(hostView, View(context), 1)
    hostView.mount(0, mountItem)
    hostView.moveItem(mountItem, 0, 100)
    Java6Assertions.assertThat(hostView.getMountItemAt(100)).isSameAs(mountItem)
  }

  @Test
  fun testSwapItems() {
    val hostView = HostView(context)
    val mountItem = createMountItem(hostView, View(context), 1)
    val mountItem2 = createMountItem(hostView, View(context), 2)
    hostView.mount(0, mountItem)
    hostView.mount(1, mountItem2)
    hostView.moveItem(mountItem, 0, 1)
    hostView.moveItem(mountItem2, 1, 0)
    Java6Assertions.assertThat(hostView.getMountItemAt(0)).isSameAs(mountItem2)
    Java6Assertions.assertThat(hostView.getMountItemAt(1)).isSameAs(mountItem)
  }

  @Test
  fun onRenderMixedHierarchy_HostViewShouldHaveExpectedState() {
    val c = renderCoreTestRule.context
    val drawTracker: MutableList<Long> = ArrayList()
    val setStateTracker: MutableList<Long> = ArrayList()
    val jumpToCurrentStateTracker: MutableList<Long> = ArrayList()
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .width(240)
            .height(240)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        DrawableWrapperUnit(
                            TrackingColorDrawable(
                                Color.BLACK,
                                2,
                                true,
                                drawTracker,
                                setStateTracker,
                                jumpToCurrentStateTracker),
                            2))
                    .width(120)
                    .height(120))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        DrawableWrapperUnit(
                            TrackingColorDrawable(
                                Color.BLUE,
                                3,
                                false,
                                drawTracker,
                                setStateTracker,
                                jumpToCurrentStateTracker),
                            3))
                    .x(5)
                    .y(5)
                    .width(110)
                    .height(110))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 4))
                    .x(10)
                    .y(10)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        DrawableWrapperUnit(
                            TrackingColorDrawable(
                                Color.BLUE,
                                5,
                                true,
                                drawTracker,
                                setStateTracker,
                                jumpToCurrentStateTracker),
                            5))
                    .x(15)
                    .y(15)
                    .width(95)
                    .height(95))
            .build()
    renderCoreTestRule.useRootNode(LayoutResultWrappingNode(root)).render()
    val host = renderCoreTestRule.rootHost as HostView
    Java6Assertions.assertThat(host.mountItemCount).describedAs("Mounted items").isEqualTo(4)
    host.dispatchDraw(Canvas())
    Java6Assertions.assertThat(drawTracker).containsExactly(2L, 3L, 5L)
    Java6Assertions.assertThat(setStateTracker).containsExactly(2L, 5L)
    setStateTracker.clear()
    host.drawableStateChanged()
    Java6Assertions.assertThat(setStateTracker).containsExactly(2L, 5L)
    Java6Assertions.assertThat(jumpToCurrentStateTracker).containsExactly(2L, 3L, 5L)
    jumpToCurrentStateTracker.clear()
    host.jumpDrawablesToCurrentState()
    Java6Assertions.assertThat(jumpToCurrentStateTracker).containsExactly(2L, 3L, 5L)
    val item = host.getMountItemAt(1)
    host.unmount(item)
    Java6Assertions.assertThat(host.mountItemCount).describedAs("Mounted items").isEqualTo(3)
    drawTracker.clear()
    host.dispatchDraw(Canvas())
    Java6Assertions.assertThat(drawTracker).containsExactly(2L, 5L)
    host.visibility = View.INVISIBLE
    Java6Assertions.assertThat((host.getMountItemAt(0).content as Drawable).isVisible)
        .describedAs("Drawable visibility is")
        .isFalse
    Java6Assertions.assertThat((host.getMountItemAt(3).content as Drawable).isVisible)
        .describedAs("Drawable visibility is")
        .isFalse
  }

  @Test
  fun onRenderUnitWithTouchableDrawable_shouldHandleTouchEvent() {
    val point = Point(0, 0)
    val unit =
        DrawableWrapperUnit(
            TouchableColorDrawable { v, event ->
              point.x = event.x.toInt()
              point.y = event.y.toInt()
              true
            },
            1)
    val root: LayoutResult =
        SimpleLayoutResult.create()
            .width(100)
            .height(100)
            .child(SimpleLayoutResult.create().renderUnit(unit).width(100).height(100))
            .build()
    renderCoreTestRule.useRootNode(LayoutResultWrappingNode(root)).render()
    val host = renderCoreTestRule.rootHost as HostView
    val event = MotionEvent.obtain(200, 300, MotionEvent.ACTION_DOWN, 10.0f, 10.0f, 0)
    host.onTouchEvent(event)
    Java6Assertions.assertThat(point.x).describedAs("touch x is").isEqualTo(10)
    Java6Assertions.assertThat(point.y).describedAs("touch y is").isEqualTo(10)
  }

  private class TestRenderUnit(renderType: RenderType) :
      RenderUnit<Any>(renderType), ContentAllocator<Any> {

    override fun createContent(context: Context): Any {
      return Any()
    }

    override val contentAllocator: ContentAllocator<Any>
      get() = this

    override val id: Long
      get() = 0
  }

  class TrackingColorDrawable(
      color: Int,
      private val id: Long,
      private val isStateful: Boolean,
      private val drawTracker: MutableList<Long>,
      private val setStateTracker: MutableList<Long>,
      private val jumpToCurrentStateTracker: MutableList<Long>
  ) : ColorDrawable(color) {

    override fun draw(canvas: Canvas) {
      super.draw(canvas)
      drawTracker.add(id)
    }

    override fun isStateful(): Boolean {
      return isStateful || super.isStateful()
    }

    override fun setState(stateSet: IntArray): Boolean {
      setStateTracker.add(id)
      return super.setState(stateSet)
    }

    override fun jumpToCurrentState() {
      jumpToCurrentStateTracker.add(id)
      super.jumpToCurrentState()
    }
  }

  class TouchableColorDrawable(private val listener: OnTouchListener) : ColorDrawable(), Touchable {
    override fun onTouchEvent(event: MotionEvent, host: View): Boolean {
      return listener.onTouch(null, event)
    }

    override fun shouldHandleTouchEvent(event: MotionEvent): Boolean {
      return true
    }
  }

  companion object {
    fun createMountItem(host: Host?, content: Any, id: Long): MountItem {
      val renderType = if (content is View) RenderType.VIEW else RenderType.DRAWABLE
      val renderUnit: RenderUnit<*> = TestRenderUnit(renderType)
      return MountItem(RenderTreeNode(null, renderUnit, null, Rect(), Rect(), 0), content)
    }
  }
}
