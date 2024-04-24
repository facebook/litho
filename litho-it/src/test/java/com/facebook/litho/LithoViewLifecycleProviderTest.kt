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

import androidx.recyclerview.widget.OrientationHelper
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisible
import com.facebook.litho.widget.LinearLayoutInfo
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.sp
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/** Tests to verify that we always prepare layouts in the same order we mount them. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
class LithoViewLifecycleProviderTest {
  @Rule @JvmField val lithoViewRule: LithoViewRule = LithoViewRule()
  lateinit var lithoLifecycleProviderDelegate: LithoVisibilityEventsControllerDelegate
  lateinit var recyclerBinder: RecyclerBinder
  lateinit var invisibleTags: MutableSet<Int>

  @Before
  fun setup() {
    ComponentsConfiguration.enableRefactorLithoVisibilityEventsController = true
    invisibleTags = mutableSetOf()
    lithoLifecycleProviderDelegate = LithoVisibilityEventsControllerDelegate()
  }

  @After
  fun breakdown() {
    ComponentsConfiguration.enableRefactorLithoVisibilityEventsController = false
  }

  @Test
  fun `test visibility events for the case of initializing LifecycleProvider in RecyclerBinder`() {
    recyclerBinder =
        RecyclerBinder.Builder()
            .layoutInfo(LinearLayoutInfo(lithoViewRule.context, OrientationHelper.VERTICAL, false))
            .lithoLifecycleProvider(lithoLifecycleProviderDelegate)
            .build(lithoViewRule.context)
    for (i in 0 until 10) {
      recyclerBinder.insertItemAt(
          i, VisibilityTrackingComponent(tag = i, invisibleTracking = invisibleTags))
    }
    val testLithoView =
        lithoViewRule.render {
          Recycler.create(lithoViewRule.context)
              .binder(recyclerBinder)
              .widthPx(100)
              .heightPx(100)
              .build()
        }

    // testing initial state
    assertThat(testLithoView.lithoView.componentTree?.lifecycleProvider?.lifecycleStatus)
        .isEqualTo(LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()

    // move to invisible
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    assertThat(invisibleTags).isEqualTo((0 until 10).toSet())

    // move to visible
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()
  }

  @Test
  fun `test visibility events for the case of initializing LifecycleProvider in ComponentTree`() {
    val componentTree =
        ComponentTree.create(
                lithoViewRule.context,
                VisibilityTrackingComponent(tag = 1, invisibleTracking = invisibleTags),
                lithoLifecycleProviderDelegate)
            .build()
    val testLithoView =
        lithoViewRule.createTestLithoView(
            componentTree = componentTree, widthPx = 100, heightPx = 100)
    testLithoView.measure()
    testLithoView.layout()

    // testing initial state
    assertThat(testLithoView.lithoView.componentTree?.lifecycleProvider?.lifecycleStatus)
        .isEqualTo(LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()

    // move to invisible
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    assertThat(invisibleTags).contains(1)
    assertThat(invisibleTags.size).isEqualTo(1)

    // move to visible
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).doesNotContain(1)
    assertThat(invisibleTags).isEmpty()
  }

  @Test
  fun `test visibility events in nested LithoView for the case of setting new LifecycleProvider to parent LithoView`() {

    val testLithoView =
        lithoViewRule.render {
          LazyList(style = Style.height(100.sp).width(100.sp)) {
            for (i in 0 until 10) {
              child(
                  component =
                      VisibilityTrackingComponent(tag = i, invisibleTracking = invisibleTags))
            }
          }
        }
    testLithoView.lithoView.subscribeComponentTreeToLifecycleProvider(
        lithoLifecycleProviderDelegate)

    // testing initial state
    assertThat(testLithoView.lithoView.componentTree?.lifecycleProvider?.lifecycleStatus)
        .isEqualTo(LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()

    // move to invisible
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    assertThat(invisibleTags).isEqualTo((0 until 10).toSet())

    // move to visible
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()
  }

  @Test
  fun `test visibility events in nested LithoView for the case of setting new LifecycleProvider to parent ComponentTree`() {
    val testLithoView =
        lithoViewRule.render {
          LazyList(style = Style.height(100.sp).width(100.sp)) {
            for (i in 0 until 10) {
              child(
                  component =
                      VisibilityTrackingComponent(tag = i, invisibleTracking = invisibleTags))
            }
          }
        }
    testLithoView.lithoView.componentTree?.subscribeToLifecycleProvider(
        lithoLifecycleProviderDelegate)
    // testing initial state
    assertThat(testLithoView.lithoView.componentTree?.lifecycleProvider?.lifecycleStatus)
        .isEqualTo(LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()

    // move to invisible
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    assertThat(invisibleTags).isEqualTo((0 until 10).toSet())

    // move to visible
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()
  }
}

class VisibilityTrackingComponent(val invisibleTracking: MutableSet<Int>, val tag: Int) :
    KComponent() {
  override fun ComponentScope.render(): Component {

    return Text(
        text = "test",
        style =
            Style.height(5.sp)
                .onVisible { invisibleTracking.remove(tag) }
                .onInvisible { invisibleTracking.add(tag) })
  }
}
