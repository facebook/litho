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

package com.facebook.litho.rangemanager

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.facebook.litho.AOSPLifecycleOwnerProvider
import com.facebook.litho.LithoVisibilityEventsController
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState
import com.facebook.litho.LithoVisibilityEventsControllerDelegate
import com.facebook.litho.LithoVisibilityEventsListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TreeVisibilityEventsControllerTest {

  private val parentController = LithoVisibilityEventsControllerDelegate()
  private val received = mutableListOf<LithoVisibilityState>()
  private val listener = LithoVisibilityEventsListener { state -> received.add(state) }

  @Test
  fun `forwards the parent's visibility changes to its listeners`() {
    val childController = TreeVisibilityEventsController.childOf(parentController)
    childController.addListener(listener)

    parentController.moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)
    parentController.moveToVisibilityState(LithoVisibilityState.HINT_VISIBLE)

    assertThat(received)
        .containsExactly(LithoVisibilityState.HINT_INVISIBLE, LithoVisibilityState.HINT_VISIBLE)
  }

  /** A tree built while the parent is hidden, or as it hides, must not report itself visible. */
  @Test
  fun `reports the parent's current state`() {
    val childController = TreeVisibilityEventsController.childOf(parentController)

    parentController.moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)

    assertThat(childController.visibilityState).isEqualTo(LithoVisibilityState.HINT_INVISIBLE)
  }

  @Test
  fun `destroy destroys its listeners without touching the parent`() {
    val childController = TreeVisibilityEventsController.childOf(parentController)
    childController.addListener(listener)

    childController.destroy()

    assertThat(received).containsExactly(LithoVisibilityState.DESTROYED)
    assertThat(childController.visibilityState).isEqualTo(LithoVisibilityState.DESTROYED)
    assertThat(parentController.visibilityState).isEqualTo(LithoVisibilityState.HINT_VISIBLE)
  }

  /** Otherwise every released tree would stay in the parent controller's listener set. */
  @Test
  fun `destroy removes it from the parent`() {
    val recordingParentController = RecordingParentController()
    val childController = TreeVisibilityEventsController.childOf(recordingParentController)
    assertThat(recordingParentController.listeners).containsExactly(childController)

    childController.destroy()

    assertThat(recordingParentController.listeners).isEmpty()
  }

  @Test
  fun `ignores the parent once destroyed`() {
    val childController = TreeVisibilityEventsController.childOf(parentController)
    childController.addListener(listener)
    childController.destroy()
    received.clear()

    parentController.moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)

    assertThat(received).isEmpty()
  }

  @Test
  fun `is destroyed when the parent is`() {
    val childController = TreeVisibilityEventsController.childOf(parentController)
    childController.addListener(listener)

    parentController.moveToVisibilityState(LithoVisibilityState.DESTROYED)

    assertThat(childController.visibilityState).isEqualTo(LithoVisibilityState.DESTROYED)
    assertThat(received).contains(LithoVisibilityState.DESTROYED)
  }

  @Test
  fun `unsubscribe stops listening to the parent without destroying`() {
    val recordingParentController = RecordingParentController()
    val childController = TreeVisibilityEventsController.childOf(recordingParentController)

    childController.unsubscribe()

    assertThat(recordingParentController.listeners).isEmpty()
    assertThat(childController.visibilityState).isEqualTo(LithoVisibilityState.HINT_VISIBLE)
  }

  /** Components read the lifecycle owner from the controller, e.g. for lifecycle-aware hooks. */
  @Test
  fun `exposes the parent's lifecycle owner`() {
    val owner =
        object : LifecycleOwner {
          override val lifecycle: Lifecycle
            get() = error("not read")
        }
    val parentControllerWithOwner =
        object :
            AOSPLifecycleOwnerProvider,
            LithoVisibilityEventsController by RecordingParentController() {
          override val lifecycleOwner: LifecycleOwner = owner
        }

    assertThat(TreeVisibilityEventsController.childOf(parentControllerWithOwner).lifecycleOwner)
        .isSameAs(owner)
  }

  private class RecordingParentController : LithoVisibilityEventsController {
    val listeners = mutableSetOf<LithoVisibilityEventsListener>()
    override val visibilityState: LithoVisibilityState = LithoVisibilityState.HINT_VISIBLE

    override fun moveToVisibilityState(newVisibilityState: LithoVisibilityState): Unit = Unit

    override fun addListener(listener: LithoVisibilityEventsListener) {
      listeners.add(listener)
    }

    override fun removeListener(listener: LithoVisibilityEventsListener) {
      listeners.remove(listener)
    }
  }
}
