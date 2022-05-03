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

package com.facebook.litho.widget

import android.content.Context
import android.view.View
import com.facebook.litho.ComponentContext
import com.facebook.litho.Mountable
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Size
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.utils.MeasureSpecUtils.atMost
import com.facebook.rendercore.utils.MeasureSpecUtils.exactly
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/** Tests to verify that we always prepare layouts in the same order we mount them. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
class RecyclerBinderPreparationOrderTest {

  @JvmField @Rule val lithoViewRule = LithoViewRule()

  @Test
  fun `default traversal order should match RecyclerView layout order`() {
    val prepareTracking = mutableListOf<Int>()
    val mountTracking = mutableListOf<Int>()

    val lithoView =
        lithoViewRule.createTestLithoView {
          LazyList {
            (1..10).forEach { tag ->
              child(
                  id = tag,
                  component =
                      PrepareTrackingMountableComponent(
                          prepareTracking = prepareTracking,
                          mountTracking = mountTracking,
                          tag = tag))
            }
          }
        }

    lithoViewRule.act(lithoView) {
      lithoView.setSizeSpecs(exactly(1000), atMost(400))
      lithoView.measure()
    }

    assertThat(prepareTracking).hasSize(10)
    assertThat(mountTracking).isEmpty()

    lithoView.layout()

    // Assert that what was mounted was also the first set of rows prepared
    assertThat(mountTracking).isEqualTo(prepareTracking.subList(0, mountTracking.size))
  }

  @Test
  fun `reverseLayout traversal order should match RecyclerView layout order`() {
    val prepareTracking = mutableListOf<Int>()
    val mountTracking = mutableListOf<Int>()

    val lithoView =
        lithoViewRule.createTestLithoView {
          LazyList(reverse = true) {
            (1..10).forEach { tag ->
              child(
                  id = tag,
                  component =
                      PrepareTrackingMountableComponent(
                          prepareTracking = prepareTracking,
                          mountTracking = mountTracking,
                          tag = tag))
            }
          }
        }

    lithoViewRule.act(lithoView) {
      lithoView.setSizeSpecs(exactly(1000), atMost(400))
      lithoView.measure()
    }

    assertThat(prepareTracking).hasSize(10)
    assertThat(mountTracking).isEmpty()

    lithoView.layout()

    // Assert that what was mounted was also the first set of rows prepared
    assertThat(mountTracking).isEqualTo(prepareTracking.subList(0, mountTracking.size))
  }
}

private class PrepareTrackingMountableComponent(
    val prepareTracking: MutableList<Int>,
    val mountTracking: MutableList<Int>,
    val tag: Int
) : MountableComponent() {
  override fun MountableComponentScope.render(): Mountable<*> {
    prepareTracking.add(tag)
    return PrepareTrackingMountable(mountTracking = mountTracking, tag = tag)
  }
}

private class PrepareTrackingMountable(val mountTracking: MutableList<Int>, val tag: Int) :
    SimpleMountable<View>() {

  override fun mount(c: Context, content: View, layoutData: Any?) {
    mountTracking.add(tag)
  }

  override fun unmount(c: Context, content: View, layoutData: Any?) = Unit

  override fun getRenderType(): RenderUnit.RenderType = RenderUnit.RenderType.VIEW

  override fun createContent(context: Context): View = View(context)

  override fun measure(
      context: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      previousLayoutData: Any?
  ): Any? {
    size.width = 100
    size.height = 100
    return null
  }
}
