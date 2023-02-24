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

import java.lang.UnsupportedOperationException
import java.util.ArrayList
import java.util.Collections

/**
 * The StateContainer implementation for Kotlin components. It tracks all the state defined by
 * useState calls for the same component. See KState for how this is being used. This is a purely
 * immutable class and it exposes utilities to create a new instance with either a new piece of
 * state or by changing the value at a given index
 */
class KStateContainer : StateContainer {

  val states: List<Any?>

  private constructor(kStateContainer: KStateContainer?, state: Any?) {
    val states: MutableList<Any?>
    if (kStateContainer != null) {
      states = ArrayList(kStateContainer.states.size + 1)
      states.addAll(kStateContainer.states)
    } else {
      states = ArrayList()
    }
    states.add(state)
    this.states = Collections.unmodifiableList(states)
  }

  private constructor(kStateContainer: KStateContainer, index: Int, newValue: Any?) {
    val states = ArrayList(kStateContainer.states)
    states[index] = newValue
    this.states = Collections.unmodifiableList(states)
  }

  override fun applyStateUpdate(stateUpdate: StateUpdate) {
    throw UnsupportedOperationException(
        "Kotlin states should not be updated through applyStateUpdate calls")
  }

  fun copyAndMutate(index: Int, newValue: Any?): KStateContainer {
    return KStateContainer(this, index, newValue)
  }

  companion object {
    @JvmStatic
    fun withNewState(kStateContainer: KStateContainer?, state: Any?): KStateContainer =
        KStateContainer(kStateContainer, state)
  }
}
