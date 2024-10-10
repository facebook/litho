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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.facebook.litho.annotations.Hook
import com.facebook.litho.lifecycle.LifecycleOwnerTreeProp
import kotlinx.coroutines.flow.StateFlow

/**
 * Uses the current value of a given [stateFlow] in a Litho Kotlin component.
 *
 * The flow will be collected on the CoroutineScope given by [getTreeProp], and will be canceled
 * when this component is detached.
 */
@Hook
fun <T> ComponentScope.useFlow(stateFlow: StateFlow<T>): T =
    useProducer(initialValue = { stateFlow.value }, stateFlow) { stateFlow.collect { update(it) } }

/**
 * Returns the current value of a given [stateFlow].
 *
 * This [StateFlow] is collected on the CoroutineScope given by [getTreeProp] as long as the
 * [Lifecycle] is at the [minActiveState] state, and collection pauses when the [Lifecycle] falls
 * below [minActiveState]. The collection is canceled when this component is detached.
 */
@Hook
fun <T> ComponentScope.useFlowWithLifecycle(
    stateFlow: StateFlow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
): T {
  val lifecycle = LifecycleOwnerTreeProp.value?.lifecycle ?: error("No LifecycleOwner found")
  return useProducer(initialValue = { stateFlow.value }, stateFlow, minActiveState) {
    lifecycle.repeatOnLifecycle(minActiveState) { stateFlow.collect { update(it) } }
  }
}

/**
 * Uses the collection of a StateFlow that is dynamically supplied by [flowBlock], which will be
 * executed whenever the given [keys] change.
 *
 * The state flow will be collected on the CoroutineScope given by [getTreeProp], and will be
 * canceled when this component is detached or if any [keys] change.
 */
@Hook
fun <T> ComponentScope.useFlow(
    initialValue: () -> T,
    vararg keys: Any?,
    flowBlock: suspend () -> StateFlow<T>,
): T = useProducer(initialValue, *keys) { flowBlock().collect { update(it) } }

/**
 * Uses the collection of a StateFlow that is dynamically supplied by [flowBlock].
 *
 * It is an error to call [ComponentScope.useFlow] without keys parameter.
 */
// This deprecated-error function shadows the varargs overload so that the varargs version is not
// used without keys parameters.
@Deprecated(USE_FLOW_NO_KEYS_ERROR, level = DeprecationLevel.ERROR)
@Suppress("unused", "UNUSED_PARAMETER")
@Hook
fun <T> ComponentScope.useFlow(initialValue: () -> T, flowBlock: suspend () -> StateFlow<T>): Unit =
    throw IllegalStateException(USE_FLOW_NO_KEYS_ERROR)

private const val USE_FLOW_NO_KEYS_ERROR =
    "useFlow must provide 'keys' parameter that determines whether the existing flow will be canceled, and the new flow will be collected"
