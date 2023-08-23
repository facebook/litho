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
import android.widget.TextView
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.MountItemsPool
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class NestedPreallocationTest {

  @get:Rule val lithoRule: LithoViewRule = LithoViewRule()

  @get:Rule
  val backgroundLayoutLooperRule: BackgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Test
  fun `preallocation in a nested list should work if parent hierarchy enables it`() {
    val pool = setupFakeMountItemPool()

    val lithoView = renderTestComponent(preallocationEnabled = true)

    backgroundLayoutLooperRule.runToEndOfTasksSync()

    LithoAssertions.assertThat(lithoView).hasVisibleText("Title")
    LithoAssertions.assertThat(lithoView).hasVisibleText("Number 1")

    Assertions.assertThat(pool.preallocationsAttempted).isEqualTo(10)
  }

  @Test
  fun `preallocation in a nested list should be disabled if parent hierarchy does not have it`() {
    val pool = setupFakeMountItemPool()

    val lithoView = renderTestComponent(preallocationEnabled = false)

    LithoAssertions.assertThat(lithoView).hasVisibleText("Title")
    LithoAssertions.assertThat(lithoView).hasVisibleText("Number 1")

    Assertions.assertThat(pool.preallocationsAttempted).isEqualTo(0)
  }

  private fun renderTestComponent(preallocationEnabled: Boolean): TestLithoView {
    val context = lithoRule.context

    val componentTree =
        ComponentTree.create(context, EmptyComponent())
            .componentsConfiguration(
                ComponentsConfiguration.create().nestedPreallocationEnabled(true).build())
            .shouldPreallocateMountContentPerMountSpec(preallocationEnabled)
            .useDefaultHandlerForContentPreallocation()
            .build()

    return lithoRule.render(heightPx = 2040, componentTree = componentTree) { TestComponent() }
  }

  private fun setupFakeMountItemPool(): FakeMountItemsPool {
    val pool = FakeMountItemsPool()
    MountItemsPool.setMountContentPoolFactory { pool }
    return pool
  }

  private class FakeMountItemsPool : MountItemsPool.ItemPool {

    var preallocationsAttempted: Int = 0

    private val itemPool = MountItemsPool.DefaultItemPool(TestComponent::class.java, 2, false)

    override fun acquire(contentAllocator: ContentAllocator<*>?): Any? {
      return itemPool.acquire(contentAllocator)
    }

    override fun release(item: Any?): Boolean {
      return itemPool.release(item)
    }

    override fun maybePreallocateContent(
        c: Context?,
        contentAllocator: ContentAllocator<*>?
    ): Boolean {
      preallocationsAttempted++
      return itemPool.maybePreallocateContent(c, contentAllocator)
    }
  }

  private class TestComponent : KComponent() {

    override fun ComponentScope.render(): Component? {
      return Column {
        child(Text("Title"))

        val items = (1..10).map { it }
        child(LazyList { children(items, { it }) { MyPrimitive(it) } })
      }
    }
  }

  private class MyPrimitive(private val number: Int, private val style: Style = Style) :
      PrimitiveComponent() {

    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      return LithoPrimitive(
          layoutBehavior = FixedSizeLayoutBehavior(100.dp, 24.dp),
          mountBehavior =
              MountBehavior(
                  contentAllocator = ViewAllocator(canPreallocate = true) { TextView(it) }) {
                    bind(number) { view ->
                      view.text = "Number $number"
                      onUnbind { view.text = null }
                    }
                  },
          style)
    }
  }
}
