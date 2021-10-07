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

import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoViewAssert.assertThat
import com.facebook.litho.widget.ImmediateLazyStateUpdateDispatchingComponent
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.SimpleStateUpdateEmulator
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec
import com.facebook.litho.widget.SimpleStateUpdateEmulatorWillRender
import com.facebook.litho.widget.SimpleStateUpdateEmulatorWillRenderSpec
import com.facebook.litho.widget.TestWrapperComponent
import java.util.ArrayList
import org.assertj.core.api.Java6Assertions
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(ParameterizedRobolectricTestRunner::class)
class StateUpdatesWithReconciliationTest(
    private val useStatelessComponent: Boolean,
    private val reuseInternalNodes: Boolean
) {
  private val originalValueOfReuseInternalNodes = ComponentsConfiguration.reuseInternalNodes
  private val originalValueOfUseStatelessComponent = ComponentsConfiguration.useStatelessComponent

  companion object {
    @ParameterizedRobolectricTestRunner.Parameters(
        name = "useStatelessComponent={0}, reuseInternalNodes={1}")
    @JvmStatic
    fun data(): List<Array<Boolean>> {
      return mutableListOf(arrayOf(false, false), arrayOf(true, false), arrayOf(true, true))
    }
  }

  @JvmField @Rule var backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @JvmField @Rule var lithoViewRule = LithoViewRule()
  @Before
  fun setup() {
    ComponentsConfiguration.reuseInternalNodes = reuseInternalNodes
    ComponentsConfiguration.useStatelessComponent = reuseInternalNodes || useStatelessComponent
  }

  @After
  fun after() {
    ComponentsConfiguration.reuseInternalNodes = originalValueOfReuseInternalNodes
    ComponentsConfiguration.useStatelessComponent = originalValueOfUseStatelessComponent
  }

  @Test
  fun `should not reuse layout when root with new props is set`() {
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()
    class TestComponent(val dummyProp: Int) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column {
          child(LayoutSpecLifecycleTester.create(context).steps(lifecycleSteps).build())
        }
      }
    }

    lithoViewRule.setRoot(TestComponent(dummyProp = 0)).measure().layout().attachToWindow()

    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    lithoViewRule.setRoot(TestComponent(dummyProp = 1))
    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun `should not reuse layout when root with same props is set`() {
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()
    class TestComponent(val dummyProp: Int) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column {
          child(LayoutSpecLifecycleTester.create(context).steps(lifecycleSteps).build())
        }
      }
    }

    lithoViewRule.setRoot(TestComponent(dummyProp = 0)).measure().layout().attachToWindow()

    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    lithoViewRule.setRoot(TestComponent(dummyProp = 0))
    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun `should reuse unaffected part of layout when sync state update occurs`() {
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()
    class TestComponent() : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column {
          child(LayoutSpecLifecycleTester.create(context).steps(lifecycleSteps).build())
          child(
              SimpleStateUpdateEmulator.create(context)
                  .caller(stateUpdater)
                  .widthPx(100)
                  .heightPx(100)
                  .prefix("Count: ")
                  .build())
        }
      }
    }

    lithoViewRule.setRoot(TestComponent()).measure().layout().attachToWindow()

    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    stateUpdater.increment()
    assertThat(LifecycleStep.getSteps(lifecycleSteps))
        .doesNotContain(LifecycleStep.ON_CREATE_LAYOUT)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count: 2")
  }

  @Test
  fun `should reuse unaffected part of layout when async state update occurs`() {
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()
    class TestComponent() : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column {
          child(LayoutSpecLifecycleTester.create(context).steps(lifecycleSteps).build())
          child(
              SimpleStateUpdateEmulator.create(context)
                  .caller(stateUpdater)
                  .widthPx(100)
                  .heightPx(100)
                  .prefix("Count: ")
                  .build())
        }
      }
    }

    lithoViewRule.setRoot(TestComponent()).measure().layout().attachToWindow()

    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    stateUpdater.incrementAsync()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()

    assertThat(LifecycleStep.getSteps(lifecycleSteps))
        .doesNotContain(LifecycleStep.ON_CREATE_LAYOUT)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count: 2")
  }

  @Test
  fun `should reuse unaffected part of layout when async state update occurs child component using will render`() {
    val stateUpdater = SimpleStateUpdateEmulatorWillRenderSpec.Caller()
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()
    class TestComponent() : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column {
          child(LayoutSpecLifecycleTester.create(context).steps(lifecycleSteps).build())
          child(
              SimpleStateUpdateEmulatorWillRender.create(context)
                  .caller(stateUpdater)
                  .widthPx(100)
                  .heightPx(100)
                  .prefix("Count: ")
                  .build())
        }
      }
    }

    lithoViewRule.setRoot(TestComponent()).measure().layout().attachToWindow()

    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    stateUpdater.incrementAsync()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()

    assertThat(LifecycleStep.getSteps(lifecycleSteps))
        .doesNotContain(LifecycleStep.ON_CREATE_LAYOUT)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count: 2")
  }

  @Test
  fun `should reuse unaffected part of layout when setRoot happens with pending state update`() {
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()
    class TestComponent(val dummyProp: Int) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column {
          child(
              com.facebook.litho.widget.LayoutSpecLifecycleTester.create(context)
                  .steps(lifecycleSteps)
                  .build())
          child(
              SimpleStateUpdateEmulator.create(context)
                  .caller(stateUpdater)
                  .widthPx(100)
                  .heightPx(100)
                  .prefix("Count: ")
                  .build())
        }
      }
    }

    lithoViewRule.setRoot(TestComponent(dummyProp = 0)).measure().layout().attachToWindow()

    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    stateUpdater.incrementAsync()

    lithoViewRule.setRoot(TestComponent(dummyProp = 0))

    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()

    assertThat(LifecycleStep.getSteps(lifecycleSteps))
        .doesNotContain(LifecycleStep.ON_CREATE_LAYOUT)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count: 2")
  }

  @Test
  fun `should not reuse layout when setRoot with new props happens with pending state update`() {
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()
    class TestComponent(val dummyProp: Int) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Column {
          child(
              com.facebook.litho.widget.LayoutSpecLifecycleTester.create(context)
                  .steps(lifecycleSteps)
                  .build())
          child(
              SimpleStateUpdateEmulator.create(context)
                  .caller(stateUpdater)
                  .widthPx(100)
                  .heightPx(100)
                  .prefix("Count: ")
                  .build())
        }
      }
    }

    lithoViewRule.setRoot(TestComponent(dummyProp = 0)).measure().layout().attachToWindow()

    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    stateUpdater.incrementAsync()

    lithoViewRule.setRoot(TestComponent(dummyProp = 1))

    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()

    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count: 2")
  }

  /**
   * In this scenario, we make sure that if a state update happens in the background followed by a
   * second state update in the background before the first can commit on the main thread, that the
   * final result includes both state updates.
   */
  @Test
  fun `should apply state updates when two different state updates occur simultaneously in the background`() {
    val defaultReuseInternalNode = ComponentsConfiguration.reuseInternalNodes
    ComponentsConfiguration.reuseInternalNodes = false
    val c = lithoViewRule.context
    lithoViewRule.setSizePx(100, 100).measure().layout().attachToWindow()
    val stateUpdater1 = SimpleStateUpdateEmulatorSpec.Caller()
    val stateUpdater2 = SimpleStateUpdateEmulatorSpec.Caller()
    lithoViewRule.setRootAsync(
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        SimpleStateUpdateEmulator.create(c)
                            .caller(stateUpdater1)
                            .widthPx(100)
                            .heightPx(100)
                            .prefix("First: "))
                    .child(
                        SimpleStateUpdateEmulator.create(c)
                            .caller(stateUpdater2)
                            .widthPx(100)
                            .heightPx(100)
                            .prefix("Second: ")))
            .build())
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()
    lithoViewRule.layout()

    // Do two state updates sequentially without draining the main thread queue
    stateUpdater1.incrementAsync()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    stateUpdater2.incrementAsync()
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper()
    lithoViewRule.layout()
    assertThat(lithoViewRule.lithoView).hasVisibleText("First: 2")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Second: 2")
    ComponentsConfiguration.reuseInternalNodes = defaultReuseInternalNode
  }

  /**
   * In this scenario, we make sure that if a state update happens in the background followed by a
   * second state update in the background before the first can commit on the main thread, that the
   * final result includes both state updates.
   */
  @Test
  fun `should apply state updates when two different state updates occur simultaneously in the background, stateless version`() {
    val defaultReuseInternalNode = ComponentsConfiguration.reuseInternalNodes
    val defaultStatelessness = ComponentsConfiguration.useStatelessComponent
    ComponentsConfiguration.reuseInternalNodes = true
    ComponentsConfiguration.useStatelessComponent = true
    val c = lithoViewRule.context
    lithoViewRule.setSizePx(100, 100).measure().layout().attachToWindow()
    val stateUpdater1 = SimpleStateUpdateEmulatorSpec.Caller()
    val stateUpdater2 = SimpleStateUpdateEmulatorSpec.Caller()
    lithoViewRule.setRootAsync(
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        SimpleStateUpdateEmulator.create(c)
                            .caller(stateUpdater1)
                            .widthPx(100)
                            .heightPx(100)
                            .prefix("First: "))
                    .child(
                        SimpleStateUpdateEmulator.create(c)
                            .caller(stateUpdater2)
                            .widthPx(100)
                            .heightPx(100)
                            .prefix("Second: ")))
            .build())
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()
    lithoViewRule.layout()

    // Do two state updates sequentially without draining the main thread queue
    stateUpdater1.incrementAsync()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    stateUpdater2.incrementAsync()
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper()
    lithoViewRule.layout()
    assertThat(lithoViewRule.lithoView).hasVisibleText("First: 2")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Second: 2")
    ComponentsConfiguration.reuseInternalNodes = defaultReuseInternalNode
    ComponentsConfiguration.useStatelessComponent = defaultStatelessness
  }

  @Test
  fun `should not reconcile when setRoot with different props with pending update`() {
    val c = lithoViewRule.context
    val child =
        com.facebook.litho.widget.ImmediateLazyStateUpdateDispatchingComponent.create(c).build()
    val initial: Component = Column.create(c).child(child).build()
    lithoViewRule.attachToWindow().setRoot(initial).measure().layout()
    val info: MutableList<StepInfo> = ArrayList()
    val updated: Component =
        Column.create(c).child(child).child(LayoutSpecLifecycleTester.create(c).steps(info)).build()
    lithoViewRule.setRoot(updated)

    // If the new component resolves then the root component (column) was not reconciled.
    Java6Assertions.assertThat(LifecycleStep.getSteps(info))
        .contains(LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun `should reconcile when setRoot called with delegating Component with pending update`() {
    val c = lithoViewRule.context
    val info: MutableList<StepInfo> = ArrayList()
    val initial: Component =
        TestWrapperComponent.create(c)
            .delegate(
                Column.create(c)
                    .child(ImmediateLazyStateUpdateDispatchingComponent.create(c))
                    .child(LayoutSpecLifecycleTester.create(c).steps(info)))
            .build()
    lithoViewRule.attachToWindow().setRoot(initial).measure().layout()
    val updated: Component =
        TestWrapperComponent.create(c)
            .delegate(
                Column.create(c)
                    .child(ImmediateLazyStateUpdateDispatchingComponent.create(c))
                    .child(LayoutSpecLifecycleTester.create(c).steps(info)))
            .build()
    info.clear()
    lithoViewRule.setRoot(updated)

    // If the new component's on create layout is not called then it was reconciled.
    assertThat(LifecycleStep.getSteps(info)).doesNotContain(LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun `ensure state is not lost when multiple different components update state`() {
    val c = lithoViewRule.context
    val caller_1 = SimpleStateUpdateEmulatorSpec.Caller()
    val caller_2 = SimpleStateUpdateEmulatorSpec.Caller()
    val root: Component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller_1))
            .child(SimpleStateUpdateEmulator.create(c).caller(caller_2))
            .build()
    lithoViewRule.attachToWindow().setRoot(root).measure().layout()

    // trigger a state update
    caller_1.increment()
    assertThat(lithoViewRule.lithoView).hasVisibleText("Text: 2")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Text: 1")

    // trigger a state update
    caller_2.increment()
    assertThat(lithoViewRule.lithoView).hasVisibleText("Text: 2")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Text: 2")

    // trigger a state update
    caller_1.increment()
    assertThat(lithoViewRule.lithoView).hasVisibleText("Text: 3")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Text: 2")
  }
}
