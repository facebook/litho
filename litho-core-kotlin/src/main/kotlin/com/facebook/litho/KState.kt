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

  // TODO: remove lateinit after Hooks experiment(with ComponentsConfiguration.isHooksImplEnabled
  // config) is complete.
  private lateinit var hooks: Hooks
  private lateinit var hookStateKey: String
  private var hookIndex: Int = 0

  init {
    c.hooksHandler?.let {
      hooks = it.getOrCreate(c.componentScope.globalKey)
      hookIndex = hooks.getAndIncrementHookIndex()
      hookStateKey = "${c.componentScope.globalKey}:$hookIndex"
    }
  }

  operator fun getValue(nothing: Nothing?, property: KProperty<*>): State<T> {
    val hooksHandler = c.hooksHandler
    @Suppress("UNCHECKED_CAST")
    return if (hooksHandler != null) {
      val value = hooksHandler.getOrPut(c.componentScope.globalKey, hookIndex) { getInitialState() }
      State(c.componentScope.globalKey, value, hookIndex)
    } else {
      hookStateKey = "${c.componentScope.globalKey}:${property.name}"
      val value = c.stateHandler!!.hookState.getOrPut(hookStateKey) { getInitialState() } as T
      State(hookStateKey, value)
    }
  }

  private fun getInitialState(): T =
      c.componentTree.initialStateContainer.createOrGetInitialHookState(hookStateKey) {
        initializer()
      }
}

/** Interface with which a component gets the value from a state or updates it. */
class State<T>(internal val key: String, private val value: T, internal val hookIndex: Int = -1) {

  fun get() = value
}

/** Allow reading value property of a state with simpler syntax */
val <T> State<T>.value: T
  get() = this.get()

/** Common interface for [StateUpdater] and [HookStateUpdater]. */
interface Updater {
  var <T> State<T>.value: T
}

/** Scope object for updating state - while in a StateUpdater block, State.value may be set. */
class StateUpdater(private val stateHandler: StateHandler) : Updater {

  override var <T> State<T>.value: T
    @Suppress("UNCHECKED_CAST") get() = stateHandler.hookState[key] as T
    set(value) {
      stateHandler.hookState[key] = value
    }
}

class HookStateUpdater(private val hooksHandler: HooksHandler) : Updater {

  override var <T> State<T>.value: T
    get() = hooksHandler.getOrCreate(key).get(hookIndex)
    set(value) {
      hooksHandler.getOrCreate(key).set(hookIndex, value)
    }
}

/**
 * Enqueues a state update block to be run before the next layout in order to update hook state.
 * Assignments to the state variables, created by [useState], are only allowed inside this block.
 */
fun DslScope.updateState(block: Updater.() -> Unit) {
  if (ComponentsConfiguration.isHooksImplEnabled) {
    context.updateHookStateAsync<HooksHandler> { hooksHandler ->
      HookStateUpdater(hooksHandler).block()
    }
  } else {
    context.updateHookStateAsync<StateHandler> { stateHandler ->
      StateUpdater(stateHandler).block()
    }
  }
}
