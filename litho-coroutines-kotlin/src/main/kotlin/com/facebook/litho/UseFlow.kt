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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
 * Returns a [Value] that reflects the current value of the given [flow].
 *
 * The flow will be collected on the CoroutineScope given by [getTreeProp], and will be canceled
 * when this component is detached.
 *
 * Unlike [useFlow], this hook returns a [Value] wrapper which enables deferred reads — the value is
 * only read when accessed, allowing more granular tracking of state dependencies.
 */
@Hook
fun <T> ComponentScope.useFlowValue(flow: StateFlow<T>): Value<T> {
  val state = useState { flow.value }
  useCoroutine(flow) { flow.collect { state.update(it) } }
  return state
}

/**
 * Returns a [Value] that reflects the current value of the given [flow].
 *
 * This [StateFlow] is collected on the CoroutineScope given by [getTreeProp] as long as the
 * [Lifecycle] is at the [minActiveState] state, and collection pauses when the [Lifecycle] falls
 * below [minActiveState]. The collection is canceled when this component is detached.
 *
 * Unlike [useFlowWithLifecycle], this hook returns a [Value] wrapper which enables deferred reads —
 * the value is only read when accessed, allowing more granular tracking of state dependencies.
 */
@Hook
fun <T> ComponentScope.useFlowValueWithLifecycle(
    flow: StateFlow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
): Value<T> {
  val lifecycle = LifecycleOwnerTreeProp.value?.lifecycle ?: error("No LifecycleOwner found")
  val state = useState { flow.value }
  useCoroutine(flow, minActiveState) {
    lifecycle.repeatOnLifecycle(minActiveState) { flow.collect { state.update(it) } }
  }
  return state
}

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

/**
 * Creates a [DynamicValue] that is bound to a [StateFlow]. The DynamicValue will automatically
 * update whenever the StateFlow emits a new value, enabling reactive UI updates without triggering
 * component re-renders.
 *
 * This is useful for binding external state sources (like ViewModels or use cases) to Litho's
 * animated properties such as [Style.alpha], enabling smooth transitions driven by StateFlow
 * emissions.
 *
 * Example usage:
 * ```
 * class MyComponent(private val alphaFlow: StateFlow<Float>) : KComponent() {
 *   override fun ComponentScope.render(): Component {
 *     val alpha = useFlowBinding(alphaFlow)
 *     return Row(style = Style.alpha(alpha)) { ... }
 *   }
 * }
 * ```
 *
 * @param flow The [StateFlow] to bind to. The initial value of the DynamicValue will be the current
 *   value of the flow.
 * @return A [DynamicValue] that updates whenever the flow emits a new value.
 */
@Hook
fun <T> ComponentScope.useFlowBinding(flow: StateFlow<T>): DynamicValue<T> {
  val dynamicValue = useState { DynamicValue(flow.value) }.value
  val coroutineScope = useComponentCoroutineScope()
  useEffect(flow) {
    val job = flow.onEach { value -> dynamicValue.set(value) }.launchIn(coroutineScope)
    onCleanup { job.cancel() }
  }
  return dynamicValue
}

private const val USE_FLOW_NO_KEYS_ERROR =
    "useFlow must provide 'keys' parameter that determines whether the existing flow will be canceled, and the new flow will be collected"
