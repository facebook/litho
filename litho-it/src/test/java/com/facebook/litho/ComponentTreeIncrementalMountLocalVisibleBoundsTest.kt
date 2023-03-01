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
import android.graphics.Color
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager.widget.ViewPager
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(LithoTestRunner::class)
class ComponentTreeIncrementalMountLocalVisibleBoundsTest {

  private val mountedRect = Rect()
  private lateinit var componentTree: ComponentTree
  private lateinit var lithoView: TestLithoView

  @Before
  fun setup() {
    val context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    componentTree =
        ComponentTree.create(
                context, SimpleMountSpecTester.create(context).color(Color.BLACK).build())
            .layoutDiffing(false)
            .build()
    lithoView = TestLithoView(context)
    lithoView.componentTree = componentTree
    Whitebox.setInternalState(componentTree, "mMainThreadLayoutState", mock<LayoutState>())
  }

  @Test
  fun testGetLocalVisibleBounds() {
    lithoView.shouldRetundCorrectedVisibleRect = Rect(10, 5, 20, 15)
    lithoView.performIncrementalMountForVisibleBoundsChange()
    assertThat(mountedRect).isEqualTo(Rect(10, 5, 20, 15))
  }

  @Test
  fun testViewPagerInHierarchy() {
    lithoView.shouldRetundCorrectedVisibleRect = null
    val listener = arrayOfNulls<ViewPager.OnPageChangeListener>(1)
    val runnable = arrayOfNulls<Runnable>(1)
    val viewPager: ViewPager =
        object : ViewPager(componentTree.context.androidContext) {
          override fun addOnPageChangeListener(l: OnPageChangeListener) {
            listener[0] = l
            super.addOnPageChangeListener(l)
          }

          override fun postOnAnimation(action: Runnable) {
            runnable[0] = action
            super.postOnAnimation(action)
          }

          override fun removeOnPageChangeListener(l: OnPageChangeListener) {
            listener[0] = null
            super.removeOnPageChangeListener(l)
          }
        }
    viewPager.addView(lithoView)
    componentTree.attach()

    // This is set to null by mComponentTree.attach(), so set it again here.
    Whitebox.setInternalState(componentTree, "mMainThreadLayoutState", mock<LayoutState>())
    assertThat(listener[0]).isNotNull
    lithoView.shouldRetundCorrectedVisibleRect = Rect(10, 5, 20, 15)
    listener[0]?.onPageScrolled(10, 10f, 10)
    assertThat(mountedRect).isEqualTo(Rect(10, 5, 20, 15))
    componentTree.detach()
    assertThat(runnable[0]).isNotNull
    runnable[0]?.run()
    assertThat(listener[0]).isNull()
  }

  /**
   * Required in order to ensure that [LithoView.mount] is mocked correctly (it needs protected
   * access to be mocked).
   */
  inner class TestLithoView(context: ComponentContext?) : LithoView(context) {
    var shouldRetundCorrectedVisibleRect: Rect? = null
    override fun mount(
        layoutState: LayoutState,
        currentVisibleArea: Rect?,
        processVisibilityOutputs: Boolean
    ) {
      if (processVisibilityOutputs) {
        mountedRect.set(currentVisibleArea ?: Rect())
      }
      // We don't actually call mount. LayoutState is a mock :(
    }

    public override fun getCorrectedLocalVisibleRect(outRect: Rect): Boolean {
      val rect = shouldRetundCorrectedVisibleRect
      if (rect != null) {
        outRect.set(rect)
        return true
      }
      return false
    }
  }
}
