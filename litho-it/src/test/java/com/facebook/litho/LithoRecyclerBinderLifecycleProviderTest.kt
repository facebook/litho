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
import android.os.Looper
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.RenderInfo
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class LithoRecyclerBinderLifecycleProviderTest {

  private lateinit var lithoLifecycleProviderDelegate: LithoVisibilityEventsControllerDelegate
  private lateinit var recyclerBinder: RecyclerBinder
  private lateinit var recyclerView: RecyclerView

  @JvmField @Rule var backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Before
  fun setup() {
    lithoLifecycleProviderDelegate = LithoVisibilityEventsControllerDelegate()
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    recyclerView = RecyclerView(ApplicationProvider.getApplicationContext())
    recyclerView.layoutParams = ViewGroup.LayoutParams(10, 100)
    recyclerBinder =
        RecyclerBinder.Builder().lithoLifecycleProvider(lithoLifecycleProviderDelegate).build(c)
    recyclerBinder.mount(recyclerView)
    val components: MutableList<RenderInfo> = ArrayList()
    val stepsList: MutableList<List<StepInfo>> = ArrayList()
    for (i in 0..19) {
      val steps: List<StepInfo> = ArrayList()
      val component =
          LayoutSpecLifecycleTester.create(c).widthPx(10).heightPx(5).steps(steps).build()
      components.add(ComponentRenderInfo.create().component(component).build())
      stepsList.add(steps)
    }
    recyclerBinder.insertRangeAt(0, components)
    recyclerBinder.notifyChangeSetComplete(true, null)
    recyclerBinder.measure(Size(), exactly(10), exactly(100), mock())
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  // All nested Lithoviews are not yet created, so there are not lifecycleproviders are created
  // either
  @Test
  fun lithoLifecycleProviderDelegateRecyclerBinderVisibleTest() {
    lithoLifecycleProviderDelegate.moveToVisibilityState(LithoVisibilityState.HINT_VISIBLE)
    for (j in 0..19) {
      assertThat(recyclerBinder.getComponentAt(j)?.lifecycleProvider?.visibilityState)
          .describedAs("Visible event is expected to be dispatched")
          .isEqualTo(LithoVisibilityState.HINT_VISIBLE)
    }
  }

  @Test
  fun lithoLifecycleProviderDelegateRecyclerBinderInvisibleTest() {
    lithoLifecycleProviderDelegate.moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)
    for (j in 0..19) {
      assertThat(recyclerBinder.getComponentAt(j)?.lifecycleProvider?.visibilityState)
          .describedAs("Invisible event is expected to be dispatched")
          .isEqualTo(LithoVisibilityState.HINT_INVISIBLE)
    }
  }
}
