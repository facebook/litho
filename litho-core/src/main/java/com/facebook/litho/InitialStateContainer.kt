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

import androidx.annotation.VisibleForTesting
import com.facebook.litho.ComponentsSystrace.beginSection
import com.facebook.litho.ComponentsSystrace.endSection
import com.facebook.litho.ComponentsSystrace.isTracing
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe

/**
 * The InitialStateContainer is a lookaside table used by a ComponentTree to create initial states
 * for Components. The idea is that the onCreateInitialState result for each component will be
 * cached and stored here so that we can guarantee it's not called multiple times on multiple
 * threads. We keep the initial states cached as long as there is a layout happening for the
 * ComponentTree. As soon as we detect that all in-flights layout have terminated we can clean up
 * the initial states cache.
 */
@ThreadSafe
class InitialStateContainer {

  // All the initial states that have been created and can not yet be released. This is a concurrent
  // map as we can access it from multiple threads. The safety is given by the fact that we will
  // only get and set for a key while holding a lock for that specific key.
  @JvmField
  @VisibleForTesting
  val initialStates = Collections.synchronizedMap(HashMap<String, StateContainer>())

  @GuardedBy("this") private val createInitialStateLocks: MutableMap<String, Any> = HashMap()

  @JvmField
  @GuardedBy("this")
  @VisibleForTesting
  var pendingStateHandlers: MutableSet<StateHandler> = HashSet()

  /**
   * Called when the ComponentTree creates a new StateHandler for a new layout computation. We keep
   * track of this new StateHandler so that we know that we need to wait for this layout computation
   * to finish before we can clear the initial states map.
   */
  @Synchronized
  fun registerStateHandler(stateHandler: StateHandler) {
    pendingStateHandlers.add(stateHandler)
  }

  /**
   * If an initial state for this component has already been created just transfers it to it.
   * Otherwise onCreateInitialState gets called for the component and its result cached.
   */
  fun createOrGetInitialStateForComponent(
      component: Component,
      scopedContext: ComponentContext,
      key: String
  ): StateContainer {
    val stateLock: Any = synchronized(this) { createInitialStateLocks.getOrPut(key) { Any() } }

    return synchronized(stateLock) {
      initialStates.getOrPut(key) {
        createInitialStateContainer(context = scopedContext, component = component)
      }
    }
  }

  private fun createInitialStateContainer(
      context: ComponentContext,
      component: Component
  ): StateContainer {
    val isTracing = isTracing
    if (isTracing) {
      beginSection("create-initial-state:${component.simpleName}")
    }
    val state = (component as SpecGeneratedComponent).createInitialStateContainer(context)
    if (isTracing) {
      endSection()
    }
    return state
  }

  fun getInitialStateForComponent(key: String): StateContainer? {
    val stateLock: Any = synchronized(this) { createInitialStateLocks.getOrPut(key) { Any() } }

    synchronized(stateLock) {
      return initialStates[key]
    }
  }

  /**
   * If an initial state for this component has already been created just return it, otherwise
   * execute the initializer and cache the result.
   */
  fun <T> createOrGetInitialHookState(
      key: String,
      hookIndex: Int,
      initializer: HookInitializer<T>,
      componentName: String,
  ): KStateContainer {
    val stateLock: Any = synchronized(this) { createInitialStateLocks.getOrPut(key) { Any() } }

    return synchronized(stateLock) {
      val initialHookStates = initialStates[key] as KStateContainer?

      // sequences are guaranteed to be used in order. If the states list size is greater than
      // hookIndex we should be guaranteed to find the state
      if (initialHookStates != null && initialHookStates.states.size > hookIndex) {
        return initialHookStates
      }

      val isTracing = isTracing
      if (isTracing) {
        beginSection("create-initial-state:${componentName}[$hookIndex]")
      }
      val initialState = initializer.init()
      if (isTracing) {
        endSection()
      }

      // If the state needed to be initialised it should be guaranteed that it needs to be added at
      // the end of the list. We create a new KStateContainer to guarantee immutability of state
      // containers.
      val hookStates = KStateContainer.withNewState(initialHookStates, initialState)
      check(hookIndex < hookStates.states.size) {
        ("Unexpected useState hook sequence encountered: $hookIndex (states size: ${hookStates.states.size}). This usually indicates that the useState hook is being called from within a conditional, loop, or after an early-exit condition. See https://fblitho.com/docs/mainconcepts/hooks-intro/#rules-for-hooks for more information on the Rules of Hooks.")
      }
      initialStates[key] = hookStates
      hookStates
    }
  }

  /**
   * Called when the ComponentTree commits a new StateHandler or discards one for a discarded layout
   * computation.
   */
  @Synchronized
  fun unregisterStateHandler(stateHandler: StateHandler) {
    pendingStateHandlers.remove(stateHandler)
    if (pendingStateHandlers.isEmpty()) {
      createInitialStateLocks.clear()
      // This is safe as we have a guarantee that by this point there is no layout happening
      // and therefore we can not be executing createOrGetInitialStateForComponent or
      // createOrGetInitialHookState from any thread.
      initialStates.clear()
    }
  }
}
