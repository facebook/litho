/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.EmptyComponent
import com.facebook.litho.widget.Text
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [useCached]. */
@Suppress("MagicNumber")
@RunWith(LithoTestRunner::class)
class KCachedTest {

  private lateinit var context: ComponentContext
  private lateinit var emptyComponent: EmptyComponent

  @Before
  fun setUp() {
    context = ComponentContext(getApplicationContext<Context>())
    emptyComponent = EmptyComponent.create(context).build()
  }

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenOneInputStayTheSame() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val root =
        KComponent {
          val expensiveString by useCached("hello") {
            initCounter.incrementAndGet()
            expensiveRepeatFunc("hello")
          }
          Text(text = expensiveString)
        }

    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenTwoInputsStayTheSame() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val root =
        KComponent {
          val expensiveString by useCached("hello", 100) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc("hello", 100)
          }
          Text(text = expensiveString)
        }

    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenInputArrayStaysTheSame() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val root =
        KComponent {
          val expensiveString by useCached("hello", 100, "litho") {
            initCounter.incrementAndGet()
            expensiveRepeatFunc("hello", 100, "litho")
          }
          Text(text = expensiveString)
        }

    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsRecalculatedWhenOneInputChange() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    val root =
        KComponent {
          val expensiveString by useCached("count" + repeatNum.get()) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc("count" + repeatNum.get())
          }
          Text(text = expensiveString)
        }

    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsRecalculatedWhenTwoInputsChange() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    val root =
        KComponent {
          val expensiveString by useCached("world", repeatNum.get()) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc("world", repeatNum.get())
          }
          Text(text = expensiveString)
        }

    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsRecalculatedWhenInputArrayChange() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    val root =
        KComponent {
          val expensiveString by useCached("world", repeatNum.get(), "litho") {
            initCounter.incrementAndGet()
            expensiveRepeatFunc("world", repeatNum.get(), "litho")
          }
          Text(text = expensiveString)
        }

    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsReusedBetweenComponentsOfSameTypeWhenInputsStayTheSame() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val root =
        KComponent {
          Row {
            +Leaf1("hello", 100, initCounter)
            Column { +Leaf1("hello", 100, initCounter) }
          }
        }

    ComponentTestHelper.mountComponent(lithoView, componentTree, root)

    // CacheValue is shared between two `Leaf1` components.
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValuesWithTheSameInputsAndNameAreNotReusedBetweenComponentsOfDifferentTypes() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val root =
        KComponent {
          Row {
            +Leaf1("hello", 100, initCounter)
            +Leaf1("hello", 100, initCounter)
            +Leaf2("hello", 100, initCounter)
          }
        }

    ComponentTestHelper.mountComponent(lithoView, componentTree, root)

    // CacheValue is shared between two `Leaf1` components, but not shared with `Leaf2`, thus
    // initCounter is 2.
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValuesWithDifferentNamesAreCalculatedAndReusedIndependentlyEvenWhenHaveSameInputs() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val root = ComponentWithTwoCachedValuesWithSameInputs("hello", 20, initCounter)
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(2)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(2)
  }

  private class Leaf1(str: String, repeatNum: Int, initCounter: AtomicInteger) :
      KComponent({
        val expensiveString by useCached(str, repeatNum) {
          initCounter.incrementAndGet()
          expensiveRepeatFunc(str, repeatNum)
        }
        Text(text = expensiveString)
      })

  private class Leaf2(str: String, repeatNum: Int, initCounter: AtomicInteger) :
      KComponent({
        val expensiveString by useCached(str, repeatNum) {
          initCounter.incrementAndGet()
          expensiveRepeatFunc(str, repeatNum)
        }
        Text(text = expensiveString)
      })

  private class ComponentWithTwoCachedValuesWithSameInputs(
      str: String,
      repeatNum: Int,
      initCounter: AtomicInteger
  ) :
      KComponent({
        val expensiveString1 by useCached(str, repeatNum) {
          initCounter.incrementAndGet()
          expensiveRepeatFunc(str, repeatNum)
        }

        val expensiveString2 by useCached(str, repeatNum) {
          initCounter.incrementAndGet()
          expensiveRepeatFunc(str, repeatNum)
        }

        Row {
          +Text(text = expensiveString1)
          +Text(text = expensiveString2)
        }
      })

  companion object {
    private fun expensiveRepeatFunc(prefix: String, num: Int = 20, suffix: String? = null): String {
      return StringBuilder()
          .apply {
            repeat(num) {
              append(prefix)
              suffix?.let { append(it) }
            }
          }
          .toString()
    }
  }
}
