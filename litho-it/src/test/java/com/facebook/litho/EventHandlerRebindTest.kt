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

import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoViewAssert
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.EventHandlerBindingComponent
import com.facebook.litho.widget.EventHandlerBindingComponentSpec
import com.facebook.litho.widget.EventHandlerBindingSection
import com.facebook.litho.widget.EventHandlerBindingSectionSpec
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.sp
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Tests to make sure we properly dispatch events to the latest versions of event handlers when
 * eventhandlers are used across trees.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class EventHandlerRebindTest {

  @Rule @JvmField val lithoViewRule: LithoViewRule = LithoViewRule()

  /**
   * This test makes sure that we re-bind EventHandlers even if the particular row the EventHandler
   * is used in doesn't get re-rendered.
   *
   * BACKGROUND
   *
   * Say we have a Section which has a button as one of its rows. Say that button invokes a
   * clickHandler which is defined on the section, and that click handler references section @State
   * (or @Props). Then, if the section updates state and the only thing that has changed on the
   * button is the click handler, we don't want to re-render that button. But we do want to make
   * sure that when it's clicked, we see the latest state. To do this, we have mechanisms in Litho (
   * [EventHandlersController]) that make sure EventHandlers point to their latest versions.
   *
   * THE TESTS
   *
   * There are 4 groups of assertions:
   * 1. Ensure things are set up properly
   * 2. Update the counter (@State on the Section): ensure the button doesn't re-render but the
   *    event handler does reflect the latest state.
   * 3. Cause the button to re-render (by changing the textSize prop) and update the counter: ensure
   *    the button re-renders as expected and that the event handler reflects the latest state again
   * 4. Update the counter again: ensure the button doesn't re-render but the event handler reflects
   *    the latest state. This is the same as (2) but makes sure things still work after the button
   *    re-renders.
   */
  @Test
  fun `test section event handlers update even if row doesn't re-render`() {
    val updater = EventHandlerBindingSectionSpec.StateUpdater()
    val clickListener = RecordingClickListener()
    val numButtonRenders = AtomicInteger()
    val buttonCreator =
        EventHandlerBindingSectionSpec.ButtonCreator {
          RecordingButton(numRenders = numButtonRenders, textSize = 10.sp)
        }

    val testLithoView =
        lithoViewRule.render(widthPx = 1000, heightPx = 1000) {
          RecyclerCollectionComponent.create(context)
              .section(
                  EventHandlerBindingSection.create(SectionContext(context))
                      .stateUpdater(updater)
                      .onButtonClickListener(clickListener)
                      .buttonCreator(buttonCreator))
              .build()
        }

    lithoViewRule.act(testLithoView) { clickOnText("Click Me") }

    // 1. First assertions: just make sure everything is set up properly.

    LithoViewAssert.assertThat(testLithoView.lithoView).hasVisibleText("Counter: 0")
    assertThat(numButtonRenders.get())
        .describedAs("There should be one initial render of the button")
        .isEqualTo(1)
    assertThat(clickListener.events)
        .describedAs("There should be one click of the button")
        .hasSize(1)
    assertThat(clickListener.events[0])
        .describedAs("The latest counter is 0, so the event should have value 0")
        .isEqualTo(0)
    clickListener.clear()

    // 2. Update the counter (@State on the Section): ensure the button doesn't re-render but the
    // event handler does reflect the latest state.

    updater.updateCounterSync(1)
    lithoViewRule.idle()
    lithoViewRule.act(testLithoView) { clickOnText("Click Me") }

    LithoViewAssert.assertThat(testLithoView.lithoView).hasVisibleText("Counter: 1")
    assertThat(numButtonRenders.get())
        .describedAs(
            "Updating the counter shouldn't cause the button to re-render (if it does, " +
                "that invalidates what this test is testing, namely that the EventHandlers update " +
                "even without a row re-rendering)")
        .isEqualTo(1)
    assertThat(clickListener.events)
        .describedAs("There should be one click of the button")
        .hasSize(1)
    assertThat(clickListener.events[0])
        .describedAs("The latest counter is 1, so the event should have value 1")
        .isEqualTo(1)
    clickListener.clear()

    // 3. Update the counter and cause the button to re-render (by changing the textSize prop):
    // ensure the button re-renders as expected and that the event handler reflects the latest
    // state again
    updater.updateCounterSync(2)
    testLithoView.setRoot(
        RecyclerCollectionComponent.create(lithoViewRule.context)
            .section(
                EventHandlerBindingSection.create(SectionContext(lithoViewRule.context))
                    .stateUpdater(updater)
                    .onButtonClickListener(clickListener)
                    .buttonCreator(
                        EventHandlerBindingSectionSpec.ButtonCreator {
                          RecordingButton(numRenders = numButtonRenders, textSize = 20.sp)
                        }))
            .build())
    lithoViewRule.idle()
    lithoViewRule.act(testLithoView) { clickOnText("Click Me") }

    LithoViewAssert.assertThat(testLithoView.lithoView).hasVisibleText("Counter: 2")
    assertThat(numButtonRenders.get())
        .describedAs(
            "Since we updated the button background, it should re-render (if it does not re-render, " +
                "that invalidates the premise of the rest of this test)")
        .isEqualTo(2)
    assertThat(clickListener.events)
        .describedAs("There should be one click of the button")
        .hasSize(1)
    assertThat(clickListener.events[0])
        .describedAs("The latest counter is 2, so the event should have value 2")
        .isEqualTo(2)
    clickListener.clear()

    // 4. Update the counter again: ensure the button doesn't re-render but the event handler
    // reflects the latest state. This is the same as (2) but makes sure things still work after the
    // button re-renders.

    updater.updateCounterSync(3)
    lithoViewRule.idle()
    lithoViewRule.act(testLithoView) { clickOnText("Click Me") }

    LithoViewAssert.assertThat(testLithoView.lithoView).hasVisibleText("Counter: 3")
    assertThat(numButtonRenders.get())
        .describedAs(
            "Updating the counter shouldn't cause the button to re-render (if it does, " +
                "that invalidates what this test is testing, namely that the EventHandlers update " +
                "even without a row re-rendering)")
        .isEqualTo(2)
    assertThat(clickListener.events)
        .describedAs("There should be one click of the button")
        .hasSize(1)

    assertThat(clickListener.events[0])
        .describedAs("The latest counter is 3, so the event should have value 3")
        .isEqualTo(3)

    clickListener.clear()
  }

  /**
   * This test makes sure that we re-bind EventHandlers even if the particular row the EventHandler
   * is used in doesn't get re-rendered. It's purposefully structured the exact same as the sections
   * version of the test above.
   *
   * BACKGROUND
   *
   * Say we have a parent Component which hosts a nested LithoView, and within that LithoView is a
   * button. Say this parent also defines a ClickHandler which is passed to the button, and that
   * click handler references @State (or @Props) of the parent. Then, if the parent updates state
   * and the only thing that has changed on the button is the click handler, we don't want to
   * re-render that button. But we do want to make sure that when it's clicked, we see the latest
   * state. To do this, we have mechanisms in Litho ([EventHandlersController]) that make sure
   * EventHandlers point to their latest versions.
   *
   * THE TESTS
   *
   * There are 4 groups of assertions:
   * 1. Ensure things are set up properly
   * 2. Update the counter (@State on the parent Component): ensure the button doesn't re-render but
   *    the event handler does reflect the latest state.
   * 3. Cause the button to re-render (by changing the textSize prop) and update the counter: ensure
   *    the button re-renders as expected and that the event handler reflects the latest state again
   * 4. Update the counter again: ensure the button doesn't re-render but the event handler reflects
   *    the latest state. This is the same as (2) but makes sure things still work after the button
   *    re-renders.
   */
  @Test
  fun `test component event handlers update even if nested tree doesn't re-render`() {
    val updater = EventHandlerBindingComponentSpec.StateUpdater()
    val clickListener = RecordingClickListener()
    val numButtonRenders = AtomicInteger()
    val buttonCreator =
        EventHandlerBindingComponentSpec.ButtonCreator {
          RecordingButton(numRenders = numButtonRenders, textSize = 10.sp)
        }

    val testLithoView =
        lithoViewRule.render(widthPx = 1000, heightPx = 1000) {
          EventHandlerBindingComponent.create(context)
              .stateUpdater(updater)
              .onButtonClickListener(clickListener)
              .buttonCreator(buttonCreator)
              .build()
        }

    lithoViewRule.act(testLithoView) { clickOnText("Click Me") }

    // 1. First assertions: just make sure everything is set up properly.

    LithoViewAssert.assertThat(testLithoView.lithoView).hasVisibleText("Counter: 0")
    assertThat(numButtonRenders.get())
        .describedAs("There should be one initial render of the button")
        .isEqualTo(1)
    assertThat(clickListener.events)
        .describedAs("There should be one click of the button")
        .hasSize(1)
    assertThat(clickListener.events[0])
        .describedAs("The latest counter is 0, so the event should have value 0")
        .isEqualTo(0)
    clickListener.clear()

    // 2. Update the counter (@State on the root component): ensure the button doesn't re-render but
    // the event handler does reflect the latest state.

    updater.updateCounterSync(1)
    lithoViewRule.idle()
    lithoViewRule.act(testLithoView) { clickOnText("Click Me") }

    LithoViewAssert.assertThat(testLithoView.lithoView).hasVisibleText("Counter: 1")
    assertThat(numButtonRenders.get())
        .describedAs(
            "Updating the counter shouldn't cause the button to re-render (if it does, " +
                "that invalidates what this test is testing, namely that the EventHandlers update " +
                "even without a row re-rendering)")
        .isEqualTo(1)
    assertThat(clickListener.events)
        .describedAs("There should be one click of the button")
        .hasSize(1)
    assertThat(clickListener.events[0])
        .describedAs("The latest counter is 1, so the event should have value 1")
        .isEqualTo(1)
    clickListener.clear()

    // 3. Update the counter and cause the button to re-render (by changing the textSize prop):
    // ensure the button re-renders as expected and that the event handler reflects the latest
    // state again
    updater.updateCounterSync(2)
    testLithoView.setRoot(
        EventHandlerBindingComponent.create(lithoViewRule.context)
            .stateUpdater(updater)
            .onButtonClickListener(clickListener)
            .buttonCreator(
                EventHandlerBindingComponentSpec.ButtonCreator {
                  RecordingButton(numRenders = numButtonRenders, textSize = 20.sp)
                })
            .build())
    lithoViewRule.idle()
    lithoViewRule.act(testLithoView) { clickOnText("Click Me") }

    LithoViewAssert.assertThat(testLithoView.lithoView).hasVisibleText("Counter: 2")
    assertThat(numButtonRenders.get())
        .describedAs(
            "Since we updated the button background, it should re-render (if it does not re-render, " +
                "that invalidates the premise of the rest of this test)")
        .isEqualTo(2)
    assertThat(clickListener.events)
        .describedAs("There should be one click of the button")
        .hasSize(1)
    assertThat(clickListener.events[0])
        .describedAs("The latest counter is 2, so the event should have value 2")
        .isEqualTo(2)
    clickListener.clear()

    // 4. Update the counter again: ensure the button doesn't re-render but the event handler
    // reflects the latest state. This is the same as (2) but makes sure things still work after the
    // button re-renders.

    updater.updateCounterSync(3)
    lithoViewRule.idle()
    lithoViewRule.act(testLithoView) { clickOnText("Click Me") }

    LithoViewAssert.assertThat(testLithoView.lithoView).hasVisibleText("Counter: 3")
    assertThat(numButtonRenders.get())
        .describedAs(
            "Updating the counter shouldn't cause the button to re-render (if it does, " +
                "that invalidates what this test is testing, namely that the EventHandlers update " +
                "even without a row re-rendering)")
        .isEqualTo(2)
    assertThat(clickListener.events)
        .describedAs("There should be one click of the button")
        .hasSize(1)

    assertThat(clickListener.events[0])
        .describedAs("The latest counter is 3, so the event should have value 3")
        .isEqualTo(3)

    clickListener.clear()
  }

  private class RecordingButton(
      private val numRenders: AtomicInteger,
      private val textSize: Dimen
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      numRenders.incrementAndGet()
      return Text(text = "Click Me", textSize = textSize)
    }
  }

  private class RecordingClickListener :
      EventHandlerBindingSectionSpec.OnButtonClickListener,
      EventHandlerBindingComponentSpec.OnButtonClickListener {
    private val _events: MutableList<Int> = mutableListOf()
    val events: List<Int>
      get() = _events.toList()

    override fun onClick(counter: Int) {
      _events.add(counter)
    }

    fun clear() {
      _events.clear()
    }
  }
}
