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

import kotlin.reflect.KProperty

/**
 * Declares a state variable within a Component. The initializer will provide the initial value if
 * it hasn't already been initialized in a previous lifecycle of the Component.
 *
 * Assignments to the state variables are allowed only in [updateState] block to batch updates and
 * trigger a UI layout only once per batch.
 */
fun <T> DslScope.useState(initializer: () -> T): StateDelegate<T> =
    StateDelegate(context, initializer)

/** Delegate to access and initialize a state variable. */
class StateDelegate<T>(private val c: ComponentContext, private val initializer: () -> T) {
  operator fun getValue(nothing: Nothing?, property: KProperty<*>): State<T> {
    val hookStateKey = "${c.componentScope.globalKey}:${property.name}"
    @Suppress("UNCHECKED_CAST")
    val value = c.stateHandler!!.hookState.getOrPut(
        hookStateKey,
        initializer) as T
    return State(hookStateKey, value)
  }
}

/** Interface with which a component gets the value from a state or updates it. */
class State<T>(internal val key: String, private val value: T) {

  fun get() = value
}

/** Allow reading value property of a state with simpler syntax */
val <T> State<T>.value: T
  get() = this.get()

/** Scope object for updating state - while in a StateUpdater block, State.value may be set. */
class StateUpdater(private val stateHandler: StateHandler) {

  var <T> State<T>.value: T
    @Suppress("UNCHECKED_CAST")
    get() = stateHandler.hookState[key] as T
    set(value) {
      stateHandler.hookState[key] = value
    }
}

/**
 * Enqueues a state update block to be run before the next layout in order to update hook state.
 * Assignments to the state variables, created by [useState], are only allowed inside this block.
 */
fun DslScope.updateState(block: StateUpdater.() -> Unit) {
  context.updateHookStateAsync { stateHandler: StateHandler ->
    StateUpdater(stateHandler).block()
  }
}
