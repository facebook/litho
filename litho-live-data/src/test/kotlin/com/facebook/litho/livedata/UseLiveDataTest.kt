// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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

package com.facebook.litho.livedata

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.facebook.litho.AOSPLithoVisibilityEventsController
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions.assertThat
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class UseLiveDataTest {

  private val fakeLifecycleOwner = FakeLifecycleOwner(Lifecycle.State.INITIALIZED)
  @get:Rule
  val rule: LithoViewRule =
      LithoViewRule(
          lithoVisibilityEventsController = {
            AOSPLithoVisibilityEventsController(fakeLifecycleOwner)
          })

  @Test
  fun `should observe initial live data value`() {
    val liveData = MutableLiveData("hello")

    val component = MyComponent(liveData)

    val lithoView = rule.render { component }

    assertThat(lithoView).hasVisibleText("hello")
  }

  @Test
  fun `should observe live data changes when in active state`() {
    val liveData = MutableLiveData("hello")

    val component = MyComponent(liveData)

    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_START)
    val lithoView = rule.render { component }

    assertThat(lithoView).hasVisibleText("hello")

    rule.act(lithoView) { liveData.value = "world" }

    assertThat(lithoView).hasVisibleText("world")
  }

  @Test
  fun `should not observe live data changes if not in active state`() {
    val liveData = MutableLiveData("hello")

    val component = MyComponent(liveData)

    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_CREATE)
    val lithoView = rule.render { component }

    assertThat(lithoView).hasVisibleText("hello")

    rule.act(lithoView) { liveData.value = "world" }

    assertThat(lithoView).doesNotHaveVisibleText("world")
  }

  @Test
  fun `should observe same live data from different components`() {
    val liveData = MutableLiveData("hello")

    val firstComponent = MyComponent(liveData)
    val secondComponent = MyComponent(liveData)

    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_START)

    // 1. in the beginning the first litho view will render the initial live data value
    val firstLithoView = rule.render { firstComponent }
    assertThat(firstLithoView).hasVisibleText("hello")

    // 2. we update the live data value, and that should be reflected in the first litho view
    liveData.value = "world"
    rule.idle()
    assertThat(firstLithoView).hasVisibleText("world")

    // 3. we render the second component (in a second litho view) and it should render the last live
    // data value
    val secondLithoView = rule.render { secondComponent }
    assertThat(secondLithoView).hasVisibleText("world")

    // 4. we update the live data value, and it should be reflected in both observing components
    liveData.value = "people"
    rule.idle()
    assertThat(firstLithoView).hasVisibleText("people")
    assertThat(secondLithoView).hasVisibleText("people")
  }

  @Test
  fun `should re-observe after becoming active again`() {
    val liveData = MutableLiveData("hello")

    val component = MyComponent(liveData)

    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_START)

    // 1. the litho view should reflect the initial live data value
    val lithoView = rule.render { component }
    assertThat(lithoView).hasVisibleText("hello")

    // 2. we change the lifecycle to an inactive state, and update the live data.
    // in this case, the live data won't emit as there is no active observer.
    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_STOP)
    liveData.value = "world"
    rule.idle()
    assertThat(lithoView).doesNotHaveVisibleText("world")

    // 3. we resume the lifecycle, and therefore it should show the latest store live data change
    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_RESUME)
    rule.idle()
    assertThat(lithoView).hasVisibleText("world")
  }

  @Test
  fun `should stop observing when lifecycle moves to destroyed`() {
    val liveData = MutableLiveData("hello")

    val component = MyComponent(liveData)

    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_START)

    val lithoView = rule.render { component }
    assertThat(lithoView).hasVisibleText("hello")
    Assertions.assertThat(liveData.hasObservers()).isTrue

    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_DESTROY)
    Assertions.assertThat(liveData.hasActiveObservers()).isFalse
  }

  @Test
  fun `should stop observing when component tree is released`() {
    val liveData = MutableLiveData("hello")

    val component = MyComponent(liveData)

    fakeLifecycleOwner.onEvent(Lifecycle.Event.ON_START)

    val componentTree =
        ComponentTree.create(
                rule.context, component, AOSPLithoVisibilityEventsController(fakeLifecycleOwner))
            .build()

    val lithoView = rule.render(componentTree = componentTree) { component }
    assertThat(lithoView).hasVisibleText("hello")
    Assertions.assertThat(liveData.hasObservers()).isTrue

    componentTree.release()
    Assertions.assertThat(liveData.hasObservers()).isFalse
  }

  private class MyComponent(private val liveData: LiveData<String>) : KComponent() {

    override fun ComponentScope.render(): Component {
      val text = useLiveData(liveData)

      return Text(text = text)
    }
  }

  private class FakeLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {

    override val lifecycle: FakeLifecycle = FakeLifecycle(this, initialState)

    fun onEvent(lifecycleEvent: Lifecycle.Event) {
      lifecycle.onEvent(lifecycleEvent)
    }
  }

  private class FakeLifecycle(private val owner: LifecycleOwner, initialState: State) :
      LifecycleRegistry(owner) {

    private var lastEvent: Event? = null

    private val observers: MutableList<LifecycleObserver> = mutableListOf()

    init {
      currentState = initialState
    }

    fun onEvent(event: Event) {
      val newState =
          when (event) {
            Event.ON_CREATE -> State.CREATED
            Event.ON_START -> State.STARTED
            Event.ON_RESUME -> State.RESUMED
            Event.ON_PAUSE -> State.CREATED
            Event.ON_STOP -> State.CREATED
            Event.ON_DESTROY -> State.DESTROYED
            else -> State.INITIALIZED
          }

      currentState = newState
      lastEvent = event

      observers.filterIsInstance<LifecycleEventObserver>().forEach {
        it.onStateChanged(owner, event)
      }
    }

    override fun addObserver(observer: LifecycleObserver) {
      observers.add(observer)

      if (observer is LifecycleEventObserver) {
        lastEvent?.let { observer.onStateChanged(owner, it) }
      }
    }

    override fun removeObserver(observer: LifecycleObserver) {
      observers.remove(observer)
    }
  }
}
