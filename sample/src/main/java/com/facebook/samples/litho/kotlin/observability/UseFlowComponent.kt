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

package com.facebook.samples.litho.kotlin.observability

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.useFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// start_example
internal class UseFlowComponent(private val viewModel: FakeFlowViewModel = FakeFlowViewModel()) :
    KComponent() {

  override fun ComponentScope.render(): Component? {
    val viewState = useFlow(viewModel.flow)

    return Column {
      child(HeaderComponent(viewState.name))
      child(
          CounterComponent(
              counter = viewState.counter,
              onCounterClick = { viewModel.onIncrementCounterClick() }))
    }
  }
}
// end_example

internal class FakeFlowViewModel {

  private var myCounter = 10
  private val name = "John"

  private val _flow = MutableStateFlow(ViewState(myCounter, name))
  internal val flow: StateFlow<ViewState> = _flow

  fun onIncrementCounterClick() {
    myCounter++
    _flow.value = ViewState(myCounter, name)
  }
}
