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

import com.facebook.litho.annotations.Hook
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope

interface StateProducerScope<T> : CoroutineScope {
  fun update(newValue: T)

  fun update(newValueFunction: (T) -> T)

  fun updateSync(newValue: T)

  fun updateSync(newValueFunction: (T) -> T)
}

private class StateProducerScopeImpl<T>(
    private val state: State<T>,
    override var coroutineContext: CoroutineContext = EmptyCoroutineContext,
) : StateProducerScope<T> {

  override fun update(newValue: T) {
    state.update(newValue)
  }

  override fun update(newValueFunction: (T) -> T) {
    state.update(newValueFunction)
  }

  override fun updateSync(newValue: T) {
    state.updateSync(newValue)
  }

  override fun updateSync(newValueFunction: (T) -> T) {
    state.updateSync(newValueFunction)
  }
}

/**
 * Creates a value that is updatable by a given [producer] coroutine. Whenever producer updates the
 * value, the component will be re-rendered.
 *
 * The [producer] will be re-run whenever the given keys change.
 *
 * The [producer] is run in the CoroutineScope specified by [getTreeProp].
 *
 * @return the latest value given by the [producer], or by [initialValue].
 */
@Hook
fun <T> ComponentScope.useProducer(
    initialValue: () -> T,
    vararg keys: Any?,
    producer: suspend StateProducerScope<T>.() -> Unit,
): T {
  val state = useState { initialValue() }
  val producerScope = useCached { StateProducerScopeImpl(state) }
  useCoroutine(*keys) {
    producerScope.coroutineContext = coroutineContext
    producerScope.producer()
  }
  return state.value
}
