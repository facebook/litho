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

import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.viewtree.ViewPredicates
import com.facebook.litho.widget.LayoutWithSizeSpecWithCachedValue
import com.facebook.litho.widget.LayoutWithSizeSpecWithCachedValueSpec
import com.facebook.rendercore.px
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class CachedValueTest {

  @JvmField @Rule val lithoViewRule = LithoViewRule()

  @Test
  fun `cached value is not calculated when input is same for main and nested tree`() {
    val initCounter = AtomicInteger(0)
    val nestedTreeInitCounter = LayoutWithSizeSpecWithCachedValueSpec.CalculateCachedValueCounter()

    val repeatNum = AtomicInteger(100)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("count" + repeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("count" + repeatNum.get())
            }
        return Column(style = Style.width(200.px).height(200.px)) {
          child(Text(text = expensiveString))
          child(
              LayoutWithSizeSpecWithCachedValue.create(context)
                  .number(repeatNum.get())
                  .counter(nestedTreeInitCounter)
                  .build())
        }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }

    // Cached value was calculated for main tree
    Assertions.assertThat(initCounter.get()).isEqualTo(1)
    Assertions.assertThat(nestedTreeInitCounter.get()).isEqualTo(1)

    // Check cached value updated in nested tree as well
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 100") }

    // Clear root component from ComponentTree.
    lithoViewRule.render(testLithoView.lithoView) { EmptyComponent() }

    // Re-set root component with same inputs
    lithoViewRule.render(testLithoView.lithoView) { TestComponent() }

    Assertions.assertThat(initCounter.get()).isEqualTo(1)
    Assertions.assertThat(nestedTreeInitCounter.get()).isEqualTo(1)
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 100") }
  }

  @Test
  fun `cached value is recalculated when input is changed for main and nested tree`() {
    val initCounter = AtomicInteger(0)
    val nestedTreeInitCounter = LayoutWithSizeSpecWithCachedValueSpec.CalculateCachedValueCounter()

    val repeatNum = AtomicInteger(100)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val expensiveString =
            useCached("count" + repeatNum.get()) {
              initCounter.incrementAndGet()
              expensiveRepeatFunc("count" + repeatNum.get())
            }
        return Column(style = Style.width(200.px).height(200.px)) {
          child(Text(text = expensiveString))
          child(
              LayoutWithSizeSpecWithCachedValue.create(context)
                  .number(repeatNum.get())
                  .counter(nestedTreeInitCounter)
                  .build())
        }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }

    // Cached value was calculated for main tree
    Assertions.assertThat(initCounter.get()).isEqualTo(1)
    Assertions.assertThat(nestedTreeInitCounter.get()).isEqualTo(1)

    // Check cached value updated in nested tree as well
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 100") }

    // Clear root component from ComponentTree.
    lithoViewRule.render(testLithoView.lithoView) { EmptyComponent() }

    // Increase repeat number.
    repeatNum.incrementAndGet()
    // Re-set root component and cache value is re-created.
    lithoViewRule.render(testLithoView.lithoView) { TestComponent() }

    Assertions.assertThat(initCounter.get()).isEqualTo(2)
    Assertions.assertThat(nestedTreeInitCounter.get()).isEqualTo(2)
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 101") }
  }

  @Test
  fun `cached value is recalculated when input is changed from nullable to non null value for nested tree component`() {
    val nestedTreeInitCounter = LayoutWithSizeSpecWithCachedValueSpec.CalculateCachedValueCounter()

    val nullableRepeatNum = AtomicReference<Integer>(null)
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column(style = Style.width(200.px).height(200.px)) {
          child(
              LayoutWithSizeSpecWithCachedValue.create(context)
                  .number(nullableRepeatNum.get()?.toInt())
                  .counter(nestedTreeInitCounter)
                  .build())
        }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }

    // Check cached value updated in nested tree as well
    Assertions.assertThat(nestedTreeInitCounter.get()).isEqualTo(1)
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: -1") }

    // Clear root component from ComponentTree.
    lithoViewRule.render(testLithoView.lithoView) { EmptyComponent() }

    // Increase repeat number.
    nullableRepeatNum.set(Integer(100))
    // Re-set root component and cache value is re-created.
    lithoViewRule.render(testLithoView.lithoView) { TestComponent() }

    Assertions.assertThat(nestedTreeInitCounter.get()).isEqualTo(2)
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 100") }
  }

  @Test
  fun `cached value is not reused between different nested trees components`() {
    val firstNestedTreeInitCounter =
        LayoutWithSizeSpecWithCachedValueSpec.CalculateCachedValueCounter()
    val secondNestedTreeInitCounter =
        LayoutWithSizeSpecWithCachedValueSpec.CalculateCachedValueCounter()

    val repeatNumFirst = AtomicInteger(100)
    val repeatNumSecond = AtomicInteger(200)

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column(style = Style.width(200.px).height(200.px)) {
          child(
              LayoutWithSizeSpecWithCachedValue.create(context)
                  .number(repeatNumFirst.get())
                  .counter(firstNestedTreeInitCounter)
                  .build())
          child(
              LayoutWithSizeSpecWithCachedValue.create(context)
                  .number(repeatNumSecond.get())
                  .counter(secondNestedTreeInitCounter)
                  .build())
        }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }

    // Check cached value updated in nested tree as well
    Assertions.assertThat(firstNestedTreeInitCounter.get()).isEqualTo(1)
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 100") }

    Assertions.assertThat(secondNestedTreeInitCounter.get()).isEqualTo(1)
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 200") }

    // Clear root component from ComponentTree.
    lithoViewRule.render(testLithoView.lithoView) { EmptyComponent() }

    // Increase repeat number.
    repeatNumFirst.incrementAndGet()
    // Re-set root component and cache value is re-created.
    lithoViewRule.render(testLithoView.lithoView) { TestComponent() }

    Assertions.assertThat(firstNestedTreeInitCounter.get()).isEqualTo(2)
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 101") }

    Assertions.assertThat(secondNestedTreeInitCounter.get()).isEqualTo(1)
    lithoViewRule.act(testLithoView) { ViewPredicates.hasVisibleText("ExpensiveCachedValue: 200") }
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
