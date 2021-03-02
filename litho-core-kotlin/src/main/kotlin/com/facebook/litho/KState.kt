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

/**
 * Declares a state variable within a Component. The initializer will provide the initial value if
 * it hasn't already been initialized in a previous lifecycle of the Component.
 *
 * Assignments to the state variables are allowed only in [updateState] block to batch updates and
 * trigger a UI layout only once per batch.
 */
fun <T> ComponentScope.useState(initializer: () -> T): State<T> {
  val globalKey = context.globalKey
  val hookIndex = useStateIndex++
  val hookKey = "$globalKey:$hookIndex"

  val value =
      context.stateHandler!!.hookState.getOrPut(hookKey) {
        context.componentTree.initialStateContainer.createOrGetInitialHookState(
            hookKey, initializer)
      } as
          T
  return State(context, hookKey, value)
}

/** Interface with which a component gets the value from a state or updates it. */
class State<T>(private val context: ComponentContext, private val hookKey: String, val value: T) {

  fun update(newValue: T) {
    context.updateHookStateAsync<StateHandler> { stateHandler ->
      stateHandler.hookState[hookKey] = newValue
    }
  }

  fun update(newValueFunction: (T) -> T) {
    context.updateHookStateAsync<StateHandler> { stateHandler ->
      stateHandler.hookState[hookKey] = newValueFunction(stateHandler.hookState[hookKey] as T)
    }
  }
}
