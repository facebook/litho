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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Unit tests for [useCached]. */
@Suppress("MagicNumber")
@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class KCachedTest {

  private lateinit var context: ComponentContext
  private lateinit var emptyComponent: EmptyComponent

  @Before
  fun setUp() {
    context = ComponentContext(getApplicationContext<Context>())
    emptyComponent = EmptyComponent()
  }

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenOneInputStayTheSame() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("hello") {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello")
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenTwoInputsStayTheSame() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("hello", 100) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello", 100)
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenInputArrayStaysTheSame() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("hello", 100, "litho") {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello", 100, "litho")
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsRecalculatedWhenOneInputChange() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("count" + repeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("count" + repeatNum.get())
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsRecalculatedWhenTwoInputsChange() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("world", repeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("world", repeatNum.get())
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsRecalculatedWhenInputArrayChange() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("world", repeatNum.get(), "litho") {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("world", repeatNum.get(), "litho")
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun `cached value with single null value that never changes only calculates once`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached(null) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello")
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun `cached value with null input is only calculated once when 2 inputs unchanged`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("hello", null) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello", 100)
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun `cached value with null inputs is only calculated once when array of inputs unchanged`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached(null, "hello", null) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello", 100, "hey")
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun `cached value is recalculated when one null input changes`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val nullableRepeatNum = AtomicReference<Integer>(null)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached(nullableRepeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("count" + nullableRepeatNum.get())
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Set repeat number to non-null value.
    nullableRepeatNum.set(Integer(100))
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun `cached value is recalculated when two inputs with nullable value change`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val nullableRepeatNum = AtomicReference<Integer>(null)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("world", nullableRepeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("world", nullableRepeatNum.get()?.toInt() ?: 0)
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Set repeat number to non-null value.
    nullableRepeatNum.set(Integer(100))
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun `cached value is recalculated when input array with nullable input changes`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val nullableRepeatNum = AtomicReference<Integer>(null)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("world", nullableRepeatNum.get(), "litho") {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("world", nullableRepeatNum.get()?.toInt() ?: 0, "litho")
            }
        return Text(text = expensiveString)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Set repeat number to non-null value.
    nullableRepeatNum.set(Integer(100))
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsNotReusedBetweenComponentsOfSameTypeWhenInputsStayTheSame() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row() {
          child(Leaf1("hello", 100, initCounter))
          child(Column() { child(Leaf1("hello", 100, initCounter)) })
        }
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())

    assertThat(initCounter.get())
        .describedAs(
            "CacheValue should not be shared between two `Leaf1` components under the same parent.")
        .isEqualTo(2)
  }

  @Test
  fun cachedValuesWithTheSameInputsAndNameAreNotReusedBetweenComponentsOfDifferentTypes() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent(val showLeaf1: Boolean, val dummyCacheBustingProp: Int) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row {
          if (showLeaf1) {
            child(Leaf1("hello", 100, initCounter))
          } else {
            child(Leaf2("hello", 100, initCounter))
          }
        }
      }
    }

    ComponentTestHelper.mountComponent(
        lithoView, componentTree, TestComponent(showLeaf1 = true, dummyCacheBustingProp = 0))
    ComponentTestHelper.mountComponent(
        lithoView, componentTree, TestComponent(showLeaf1 = true, dummyCacheBustingProp = 1))

    assertThat(initCounter.get()).isEqualTo(1)

    ComponentTestHelper.mountComponent(
        lithoView, componentTree, TestComponent(showLeaf1 = false, dummyCacheBustingProp = 2))

    assertThat(initCounter.get())
        .describedAs("CacheValue should not be shared between two components of different types")
        .isEqualTo(2)
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

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenOneInputStayTheSameForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestPrimitiveComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("hello") {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello")
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestPrimitiveComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestPrimitiveComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenTwoInputsStayTheSameForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("hello", 100) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello", 100)
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsCalculatedOnlyOnceWhenInputArrayStaysTheSameForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("hello", 100, "litho") {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello", 100, "litho")
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun cachedValueIsRecalculatedWhenOneInputChangeForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("count" + repeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("count" + repeatNum.get())
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsRecalculatedWhenTwoInputsChangeForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("world", repeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("world", repeatNum.get())
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsRecalculatedWhenInputArrayChangeForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val repeatNum = AtomicInteger(100)
    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("world", repeatNum.get(), "litho") {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("world", repeatNum.get(), "litho")
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun `cached value with single null value that never changes only calculates once for primitive`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached(null) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello")
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun `cached value with null input is only calculated once when 2 inputs unchanged for primitive`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("hello", null) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello", 100)
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun `cached value with null inputs is only calculated once when array of inputs unchanged for Primitive`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached(null, "hello", null) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("hello", 100, "hey")
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)
  }

  @Test
  fun `cached value is recalculated when one null input changes for primitive`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val nullableRepeatNum = AtomicReference<Integer>(null)
    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached(nullableRepeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("count" + nullableRepeatNum.get())
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Set repeat number to non-null value.
    nullableRepeatNum.set(Integer(100))
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun `cached value is recalculated when two inputs with nullable value change for primitive`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val nullableRepeatNum = AtomicReference<Integer>(null)
    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("world", nullableRepeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("world", nullableRepeatNum.get()?.toInt() ?: 0)
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Set repeat number to non-null value.
    nullableRepeatNum.set(Integer(100))
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun `cached value is recalculated when input array with nullable input changes for primitive`() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val nullableRepeatNum = AtomicReference<Integer>(null)
    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val expensiveString =
            useCached("world", nullableRepeatNum.get(), "litho") {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("world", nullableRepeatNum.get()?.toInt() ?: 0, "litho")
            }
        return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(1)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Set repeat number to non-null value.
    nullableRepeatNum.set(Integer(100))
    // Re-set root component and cache value is re-created.
    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())
    assertThat(initCounter.get()).isEqualTo(2)
  }

  @Test
  fun cachedValueIsNotReusedBetweenComponentsOfSameTypeWhenInputsStayTheSameForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row() {
          child(PrimitiveLeaf1("hello", 100, initCounter))
          child(Column() { child(PrimitiveLeaf1("hello", 100, initCounter)) })
        }
      }
    }

    ComponentTestHelper.mountComponent(lithoView, componentTree, TestComponent())

    assertThat(initCounter.get())
        .describedAs(
            "CacheValue should not be shared between two `Leaf1` components under the same parent.")
        .isEqualTo(2)
  }

  @Test
  fun cachedValuesWithTheSameInputsAndNameAreNotReusedBetweenComponentsOfDifferentTypesForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    class TestComponent(val showLeaf1: Boolean, val dummyCacheBustingProp: Int) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row {
          if (showLeaf1) {
            child(PrimitiveLeaf1("hello", 100, initCounter))
          } else {
            child(PrimitiveLeaf2("hello", 100, initCounter))
          }
        }
      }
    }

    ComponentTestHelper.mountComponent(
        lithoView, componentTree, TestComponent(showLeaf1 = true, dummyCacheBustingProp = 0))
    ComponentTestHelper.mountComponent(
        lithoView, componentTree, TestComponent(showLeaf1 = true, dummyCacheBustingProp = 1))

    assertThat(initCounter.get()).isEqualTo(1)

    ComponentTestHelper.mountComponent(
        lithoView, componentTree, TestComponent(showLeaf1 = false, dummyCacheBustingProp = 2))

    assertThat(initCounter.get())
        .describedAs("CacheValue should not be shared between two components of different types")
        .isEqualTo(2)
  }

  @Test
  fun cachedValuesWithDifferentNamesAreCalculatedAndReusedIndependentlyEvenWhenHaveSameInputsForPrimitive() {
    val initCounter = AtomicInteger(0)
    val lithoView = LithoView(context.androidContext)
    val componentTree = ComponentTree.create(context).build()

    val root = PrimitiveComponentWithTwoCachedValuesWithSameInputs("hello", 20, initCounter)
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(2)

    // Clear root component from ComponentTree.
    ComponentTestHelper.mountComponent(lithoView, componentTree, emptyComponent)

    // Re-set root component and verify expensive function isn't called.
    ComponentTestHelper.mountComponent(lithoView, componentTree, root)
    assertThat(initCounter.get()).isEqualTo(2)
  }

  private class Leaf1(val str: String, val repeatNum: Int, val initCounter: AtomicInteger) :
      KComponent() {
    override fun ComponentScope.render(): Component? {
      val expensiveString =
          useCached(str, repeatNum) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc(str, repeatNum)
          }
      return Text(text = expensiveString)
    }
  }

  private class Leaf2(val str: String, val repeatNum: Int, val initCounter: AtomicInteger) :
      KComponent() {
    override fun ComponentScope.render(): Component? {
      val expensiveString =
          useCached(str, repeatNum) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc(str, repeatNum)
          }
      return Text(text = expensiveString)
    }
  }

  private class ComponentWithTwoCachedValuesWithSameInputs(
      val str: String,
      val repeatNum: Int,
      val initCounter: AtomicInteger
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val expensiveString1 =
          useCached(str, repeatNum) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc(str, repeatNum)
          }

      val expensiveString2 =
          useCached(str, repeatNum) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc(str, repeatNum)
          }

      return Row() {
        child(Text(text = expensiveString1))
        child(Text(text = expensiveString2))
      }
    }
  }

  private class PrimitiveLeaf1(
      val str: String,
      val repeatNum: Int,
      val initCounter: AtomicInteger
  ) : PrimitiveComponent() {
    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      val expensiveString =
          useCached(str, repeatNum) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc(str, repeatNum)
          }
      return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
    }
  }

  private class PrimitiveLeaf2(
      val str: String,
      val repeatNum: Int,
      val initCounter: AtomicInteger
  ) : PrimitiveComponent() {
    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      val expensiveString =
          useCached(str, repeatNum) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc(str, repeatNum)
          }
      return LithoPrimitive(TestTextPrimitive(text = expensiveString), null)
    }
  }

  private class PrimitiveComponentWithTwoCachedValuesWithSameInputs(
      val str: String,
      val repeatNum: Int,
      val initCounter: AtomicInteger
  ) : PrimitiveComponent() {
    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      val expensiveString1 =
          useCached(str, repeatNum) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc(str, repeatNum)
          }

      val expensiveString2 =
          useCached(str, repeatNum) {
            initCounter.incrementAndGet()
            expensiveRepeatFunc(str, repeatNum)
          }

      return LithoPrimitive(
          TestTextPrimitive(text = expensiveString1, tag = expensiveString2), null)
    }
  }

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
