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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Unit tests for [useEffect]. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class KEffectsTest {

  @Rule @JvmField val lithoViewRule = LegacyLithoViewRule()

  @Test
  fun useEffect_createdThenReleased_callbacksAreInvoked() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>) : KComponent() {
      override fun ComponentScope.render(): Component? {
        useEffect(Unit) {
          useEffectCalls.add("attach")
          onCleanup { useEffectCalls.add("detach") }
        }
        return Row()
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls) }
        .attachToWindow()
        .measure()
        .layout()
        .detachFromWindow()
        .release()

    assertThat(useEffectCalls).containsExactly("attach", "detach")
  }

  @Test
  fun useEffect_anyParam_callbacksAreInvokedOnUpdate() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>, val seq: Int) : KComponent() {
      override fun ComponentScope.render(): Component? {
        useEffect(Any()) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return Row()
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, seq = 1))

    assertThat(useEffectCalls).containsExactly("detach 0", "attach 1")
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 1")
  }

  @Test
  fun useEffect_withDeps_callbacksAreInvokedOnlyIfDepsChange() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>, val dep: Int, val seq: Int) :
        KComponent() {
      override fun ComponentScope.render(): Component? {
        useEffect(dep) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return Row()
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, dep = 0, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep = 0, seq = 1))

    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep = 1, seq = 2))

    assertThat(useEffectCalls).containsExactly("detach 0", "attach 2")
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 2")
  }

  @Test
  fun useEffect_withNullableDeps_callbacksAreInvokedOnlyIfDepsChange() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>, val dep: Int?, val seq: Int) :
        KComponent() {
      override fun ComponentScope.render(): Component? {
        useEffect(dep) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return Row()
      }
    }

    val useEffectCalls = mutableListOf<String>()

    lithoViewRule.render { UseEffectComponent(useEffectCalls, dep = null, seq = 0) }
    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.render { UseEffectComponent(useEffectCalls, dep = null, seq = 1) }
    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.render { UseEffectComponent(useEffectCalls, dep = 1, seq = 2) }
    assertThat(useEffectCalls).containsExactly("detach 0", "attach 2")
    useEffectCalls.clear()

    lithoViewRule.render { UseEffectComponent(useEffectCalls, dep = null, seq = 3) }
    assertThat(useEffectCalls).containsExactly("detach 2", "attach 3")
    useEffectCalls.clear()

    lithoViewRule.release()
    assertThat(useEffectCalls).containsExactly("detach 3")
  }

  @Test
  fun useEffect_withMultipleDeps_callbacksAreInvokedIfAnyDepsChange() {
    class UseEffectComponent(
        val useEffectCalls: MutableList<String>,
        val dep1: Int,
        val dep2: Int,
        val seq: Int,
    ) : KComponent() {
      override fun ComponentScope.render(): Component? {
        useEffect(dep1, dep2) {
          useEffectCalls.add("attach $dep1:$dep2:$seq")
          onCleanup { useEffectCalls.add("detach $dep1:$dep2:$seq") }
        }
        return Row()
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 0, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0:0:0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 0, seq = 1))

    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 1, seq = 2))

    assertThat(useEffectCalls).containsExactly("detach 0:0:0", "attach 0:1:2")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 1, dep2 = 1, seq = 3))

    assertThat(useEffectCalls).containsExactly("detach 0:1:2", "attach 1:1:3")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 2, dep2 = 2, seq = 4))

    assertThat(useEffectCalls).containsExactly("detach 1:1:3", "attach 2:2:4")
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 2:2:4")
  }

  @Test
  fun useEffect_multipleUseEffectCalls_callbacksAreInvokeIndependently() {
    class UseEffectComponent(
        val useEffectCalls: MutableList<String>,
        val dep1: Int,
        val dep2: Int,
        val dep3: Int,
        val seq: Int,
    ) : KComponent() {
      override fun ComponentScope.render(): Component? {
        useEffect(dep1) {
          useEffectCalls.add("dep1: attach $seq")
          onCleanup { useEffectCalls.add("dep1: detach $seq") }
        }
        useEffect(dep2) {
          useEffectCalls.add("dep2: attach $seq")
          onCleanup { useEffectCalls.add("dep2: detach $seq") }
        }
        useEffect(dep3) {
          useEffectCalls.add("dep3: attach $seq")
          onCleanup { useEffectCalls.add("dep3: detach $seq") }
        }
        return Row()
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 0, dep3 = 0, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("dep1: attach 0", "dep2: attach 0", "dep3: attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 1, dep3 = 0, seq = 1))

    assertThat(useEffectCalls).containsExactly("dep2: detach 0", "dep2: attach 1")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 1, dep2 = 1, dep3 = 1, seq = 2))

    assertThat(useEffectCalls)
        .containsExactly("dep1: detach 0", "dep1: attach 2", "dep3: detach 0", "dep3: attach 2")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 1, dep2 = 1, dep3 = 1, seq = 3))

    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("dep1: detach 2", "dep2: detach 1", "dep3: detach 2")
  }

  @Test
  fun useEffect_unitParam_onUpdate_callbacksOnlyInvokedOnRelease() {
    class UseEffectComponent(
        val useEffectCalls: MutableList<String>,
        val seq: Int,
    ) : KComponent() {
      override fun ComponentScope.render(): Component? {
        useEffect(Unit) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return Row()
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, seq = 1))

    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 0")
  }

  @Test
  fun useEffect_createdThenReleased_callbacksAreInvoked_forPrimitives() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>) : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        useEffect(Unit) {
          useEffectCalls.add("attach")
          onCleanup { useEffectCalls.add("detach") }
        }
        return LithoPrimitive(TestPrimitive(), null)
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls) }
        .attachToWindow()
        .measure()
        .layout()
        .detachFromWindow()
        .release()

    assertThat(useEffectCalls).containsExactly("attach", "detach")
  }

  @Test
  fun useEffect_anyParams_callbacksAreInvokedOnUpdate_forPrimitive() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>, val seq: Int) :
        PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        useEffect(Any()) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return LithoPrimitive(TestPrimitive(), null)
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, seq = 1))

    assertThat(useEffectCalls).containsExactly("detach 0", "attach 1")
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 1")
  }

  @Test
  fun useEffect_noParams_callbacksAreInvokedOnUpdate_forPrimitive() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>, val seq: Int) :
        PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        useEffect(Any()) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return LithoPrimitive(TestPrimitive(), null)
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, seq = 1))

    assertThat(useEffectCalls).containsExactly("detach 0", "attach 1")
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 1")
  }

  @Test
  fun useEffect_withDeps_callbacksAreInvokedOnlyIfDepsChange_forPrimitive() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>, val dep: Int, val seq: Int) :
        PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        useEffect(dep) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return LithoPrimitive(TestPrimitive(), null)
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, dep = 0, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep = 0, seq = 1))

    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep = 1, seq = 2))

    assertThat(useEffectCalls).containsExactly("detach 0", "attach 2")
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 2")
  }

  @Test
  fun useEffect_withNullableDeps_callbacksAreInvokedOnlyIfDepsChange_forPrimitive() {
    class UseEffectComponent(val useEffectCalls: MutableList<String>, val dep: Int?, val seq: Int) :
        PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        useEffect(dep) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return LithoPrimitive(TestPrimitive(), null)
      }
    }

    val useEffectCalls = mutableListOf<String>()

    lithoViewRule.render { UseEffectComponent(useEffectCalls, dep = null, seq = 0) }
    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.render { UseEffectComponent(useEffectCalls, dep = null, seq = 1) }
    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.render { UseEffectComponent(useEffectCalls, dep = 1, seq = 2) }
    assertThat(useEffectCalls).containsExactly("detach 0", "attach 2")
    useEffectCalls.clear()

    lithoViewRule.render { UseEffectComponent(useEffectCalls, dep = null, seq = 3) }
    assertThat(useEffectCalls).containsExactly("detach 2", "attach 3")
    useEffectCalls.clear()

    lithoViewRule.release()
    assertThat(useEffectCalls).containsExactly("detach 3")
  }

  @Test
  fun useEffect_withMultipleDeps_callbacksAreInvokedIfAnyDepsChange_forPrimitive() {
    class UseEffectComponent(
        val useEffectCalls: MutableList<String>,
        val dep1: Int,
        val dep2: Int,
        val seq: Int,
    ) : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        useEffect(dep1, dep2) {
          useEffectCalls.add("attach $dep1:$dep2:$seq")
          onCleanup { useEffectCalls.add("detach $dep1:$dep2:$seq") }
        }
        return LithoPrimitive(TestPrimitive(), null)
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 0, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0:0:0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 0, seq = 1))

    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 1, seq = 2))

    assertThat(useEffectCalls).containsExactly("detach 0:0:0", "attach 0:1:2")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 1, dep2 = 1, seq = 3))

    assertThat(useEffectCalls).containsExactly("detach 0:1:2", "attach 1:1:3")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 2, dep2 = 2, seq = 4))

    assertThat(useEffectCalls).containsExactly("detach 1:1:3", "attach 2:2:4")
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 2:2:4")
  }

  @Test
  fun useEffect_multipleUseEffectCalls_callbacksAreInvokeIndependently_forPrimitive() {
    class UseEffectComponent(
        val useEffectCalls: MutableList<String>,
        val dep1: Int,
        val dep2: Int,
        val dep3: Int,
        val seq: Int,
    ) : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        useEffect(dep1) {
          useEffectCalls.add("dep1: attach $seq")
          onCleanup { useEffectCalls.add("dep1: detach $seq") }
        }
        useEffect(dep2) {
          useEffectCalls.add("dep2: attach $seq")
          onCleanup { useEffectCalls.add("dep2: detach $seq") }
        }
        useEffect(dep3) {
          useEffectCalls.add("dep3: attach $seq")
          onCleanup { useEffectCalls.add("dep3: detach $seq") }
        }
        return LithoPrimitive(TestPrimitive(), null)
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 0, dep3 = 0, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("dep1: attach 0", "dep2: attach 0", "dep3: attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 0, dep2 = 1, dep3 = 0, seq = 1))

    assertThat(useEffectCalls).containsExactly("dep2: detach 0", "dep2: attach 1")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 1, dep2 = 1, dep3 = 1, seq = 2))

    assertThat(useEffectCalls)
        .containsExactly("dep1: detach 0", "dep1: attach 2", "dep3: detach 0", "dep3: attach 2")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, dep1 = 1, dep2 = 1, dep3 = 1, seq = 3))

    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("dep1: detach 2", "dep2: detach 1", "dep3: detach 2")
  }

  @Test
  fun useEffect_unitParam_onUpdate_callbacksOnlyInvokedOnRelease_forPrimitive() {
    class UseEffectComponent(
        val useEffectCalls: MutableList<String>,
        val seq: Int,
    ) : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        useEffect(Unit) {
          useEffectCalls.add("attach $seq")
          onCleanup { useEffectCalls.add("detach $seq") }
        }
        return LithoPrimitive(TestPrimitive(), null)
      }
    }

    val useEffectCalls = mutableListOf<String>()
    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseEffectComponent(useEffectCalls, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(useEffectCalls).containsExactly("attach 0")
    useEffectCalls.clear()

    lithoViewRule.setRoot(UseEffectComponent(useEffectCalls, seq = 1))

    assertThat(useEffectCalls).isEmpty()
    useEffectCalls.clear()

    lithoViewRule.release()

    assertThat(useEffectCalls).containsExactly("detach 0")
  }

  @Test
  fun `effects should work with components that render to null`() {
    var renderCount = 0
    var attachCount = 0
    var detachCount = 0
    val controller = TestCounterComponent.Controller()
    val component =
        TestCounterComponent(
            initialCount = 2,
            onRender = { renderCount++ },
            onAttach = { attachCount++ },
            onDetach = { detachCount++ },
            controller = controller,
        )

    lithoViewRule.render { Column { child(component) } }

    // first render
    assertThat(renderCount).isEqualTo(1)
    lithoViewRule.findViewWithText("count: 2")
    assertThat(attachCount).isEqualTo(1)
    assertThat(detachCount).isEqualTo(0)

    // state update
    controller.decrement!!()
    lithoViewRule.findViewWithText("count: 1")
    assertThat(renderCount).isEqualTo(2)
    assertThat(attachCount).isEqualTo(1)
    assertThat(detachCount).isEqualTo(0)

    // state update (renders to null)
    controller.decrement!!.invoke()
    assertThat(renderCount).isEqualTo(3)
    assertThat(attachCount).isEqualTo(1)
    assertThat(detachCount).isEqualTo(0)

    // state update
    controller.increment!!.invoke()
    lithoViewRule.findViewWithText("count: 1")
    assertThat(renderCount).isEqualTo(4)
    assertThat(attachCount).isEqualTo(1)
    assertThat(detachCount).isEqualTo(0)

    // remove component
    lithoViewRule.render { Text(text = "hello") }
    lithoViewRule.findViewWithText("hello")
    assertThat(renderCount).isEqualTo(4)
    assertThat(attachCount).isEqualTo(1)
    assertThat(detachCount).isEqualTo(1)
  }
}

class TestCounterComponent(
    private val initialCount: Int = 0,
    private val onRender: () -> Unit,
    private val onAttach: () -> Unit,
    private val onDetach: () -> Unit,
    private val controller: Controller,
) : KComponent() {
  override fun ComponentScope.render(): Component? {

    val count = useState { initialCount }

    onRender()

    useEffect(onAttach, onDetach, controller) {
      onAttach()
      controller.increment = { count.updateSync { count -> count + 1 } }
      controller.decrement = { count.updateSync { count -> count - 1 } }
      CleanupFunc {
        onDetach()
        controller.increment = null
        controller.decrement = null
      }
    }

    return if (count.value > 0) {
      Text("count: ${count.value}")
    } else {
      null
    }
  }

  class Controller {
    var increment: (() -> Unit)? = null
    var decrement: (() -> Unit)? = null
  }
}

@Suppress("TestFunctionName")
internal fun PrimitiveComponentScope.TestPrimitive(): Primitive {
  return Primitive(
      layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
      mountBehavior = MountBehavior(DrawableAllocator { ColorDrawable(Color.RED) }) {})
}
