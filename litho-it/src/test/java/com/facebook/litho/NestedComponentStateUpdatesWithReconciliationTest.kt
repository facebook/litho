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

import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoViewAssert.assertThat
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.Text
import java.util.concurrent.atomic.AtomicInteger
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
class NestedComponentStateUpdatesWithReconciliationTest(private val reuseInternalNodes: Boolean) {

  @JvmField @Rule var backgroundLayoutLooperRule = BackgroundLayoutLooperRule()
  @JvmField @Rule var lithoViewRule = LithoViewRule()

  private val originalValueOfReuseInternalNodes = ComponentsConfiguration.reuseInternalNodes
  private val originalValueOfUseStatelessComponent = ComponentsConfiguration.useStatelessComponent

  companion object {
    @ParameterizedRobolectricTestRunner.Parameters(name = "reuseInternalNodes={0}")
    @JvmStatic
    fun data(): List<Array<Boolean>> {
      return mutableListOf(arrayOf(true), arrayOf(false))
    }
  }

  @Before
  fun setup() {
    ComponentsConfiguration.reuseInternalNodes = reuseInternalNodes
    ComponentsConfiguration.useStatelessComponent = reuseInternalNodes
  }

  @After
  fun after() {
    ComponentsConfiguration.reuseInternalNodes = originalValueOfReuseInternalNodes
    ComponentsConfiguration.useStatelessComponent = originalValueOfUseStatelessComponent
  }

  /*

    root
     /\
    A  B
       /\
     [C] D

  */
  @Test
  fun `when component C is updated sync, A, B, and D should not re-render`() {
    val renderCounts = RenderCounts()
    lithoViewRule
        .setRoot(RootComponent(renderCounts = renderCounts, asyncUpdates = false))
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("C").performClick()

    assertThat(renderCounts.A).hasValue(1)
    assertThat(renderCounts.B).hasValue(1)
    assertThat(renderCounts.C).hasValue(2)
    assertThat(renderCounts.D).hasValue(1)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[A]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[B]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[C]: 1")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[D]: 0")
  }

  /*

    root
     /\
    A  B
       /\
     [C] D

  */
  @Test
  fun `when component C is updated async, A, B, and D should not re-render`() {
    val renderCounts = RenderCounts()
    lithoViewRule
        .setRoot(RootComponent(renderCounts = renderCounts, asyncUpdates = true))
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("C").performClick()

    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()

    assertThat(renderCounts.A).hasValue(1)
    assertThat(renderCounts.B).hasValue(1)
    assertThat(renderCounts.C).hasValue(2)
    assertThat(renderCounts.D).hasValue(1)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[A]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[B]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[C]: 1")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[D]: 0")
  }

  /*

    root
     /\
    A  B
       /\
      C [D]

  */
  @Test
  fun `when component D is updated sync, A, B, and C should not re-render`() {
    val renderCounts = RenderCounts()
    lithoViewRule
        .setRoot(RootComponent(renderCounts = renderCounts, asyncUpdates = false))
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("D").performClick()

    assertThat(renderCounts.A).hasValue(1)
    assertThat(renderCounts.B).hasValue(1)
    assertThat(renderCounts.C).hasValue(1)
    assertThat(renderCounts.D).hasValue(2)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[A]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[B]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[C]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[D]: 1")
  }

  /*

    root
     /\
    A  B
       /\
      C [D]

  */
  @Test
  fun `when component D is updated async, A, B, and C should not re-render`() {
    val renderCounts = RenderCounts()
    lithoViewRule
        .setRoot(RootComponent(renderCounts = renderCounts, asyncUpdates = true))
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("D").performClick()

    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()

    assertThat(renderCounts.A).hasValue(1)
    assertThat(renderCounts.B).hasValue(1)
    assertThat(renderCounts.C).hasValue(1)
    assertThat(renderCounts.D).hasValue(2)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[A]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[B]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[C]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[D]: 1")
  }

  /*

    root
     /\
    A  B
       /\
     [C][D]

  */
  @Test
  fun `when components C and D are updated async in the same frame, A and B should not re-render`() {
    val renderCounts = RenderCounts()
    lithoViewRule
        .setRoot(RootComponent(renderCounts = renderCounts, asyncUpdates = true))
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("C").performClick()
    lithoViewRule.findViewWithTag("D").performClick()

    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.idleMainLooper()

    assertThat(renderCounts.A).hasValue(1)
    assertThat(renderCounts.B).hasValue(1)
    assertThat(renderCounts.C).hasValue(2)
    assertThat(renderCounts.D).hasValue(2)

    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[A]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[B]: 0")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[C]: 1")
    assertThat(lithoViewRule.lithoView).hasVisibleText("Count[D]: 1")
  }

  class RenderCounts {
    val A = AtomicInteger(0)
    val B = AtomicInteger(0)
    val C = AtomicInteger(0)
    val D = AtomicInteger(0)
  }

  private class StateUpdatingLeafComponent(
      val viewTag: String,
      val renderCount: AtomicInteger,
      val asyncUpdates: Boolean
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val clickCount = useState { 0 }

      renderCount.incrementAndGet()

      return Text(
          style =
              Style.viewTag(viewTag).onClick {
                if (asyncUpdates) {
                  clickCount.update { value -> value + 1 }
                } else {
                  clickCount.updateSync { value -> value + 1 }
                }
              },
          text = "Count[$viewTag]: ${clickCount.value}")
    }
  }

  private class BComponent(val renderCounts: RenderCounts, val asyncUpdates: Boolean) :
      KComponent() {
    override fun ComponentScope.render(): Component? {
      val clickCount = useState { 0 }

      renderCounts.B.incrementAndGet()

      return Column {
        child(
            StateUpdatingLeafComponent(
                viewTag = "C", renderCount = renderCounts.C, asyncUpdates = asyncUpdates))
        child(
            StateUpdatingLeafComponent(
                viewTag = "D", renderCount = renderCounts.D, asyncUpdates = asyncUpdates))
        child(
            Text(
                style =
                    Style.viewTag("B").onClick {
                      if (asyncUpdates) {
                        clickCount.update { value -> value + 1 }
                      } else {
                        clickCount.updateSync { value -> value + 1 }
                      }
                    },
                text = "Count[B]: ${clickCount.value}"))
      }
    }
  }

  /*
    Our goal is to build this hierarchy where you can click on any of A, B, C, D and cause them to
    perform a state update:

    root
     /\
    A  B
       /\
      C  D

  */
  private class RootComponent(val renderCounts: RenderCounts, val asyncUpdates: Boolean) :
      KComponent() {
    override fun ComponentScope.render(): Component? {
      return Column {
        child(
            StateUpdatingLeafComponent(
                viewTag = "A", renderCount = renderCounts.A, asyncUpdates = asyncUpdates))
        child(BComponent(renderCounts = renderCounts, asyncUpdates = asyncUpdates))
      }
    }
  }
}
