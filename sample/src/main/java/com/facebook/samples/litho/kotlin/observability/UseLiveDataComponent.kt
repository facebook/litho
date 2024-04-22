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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.livedata.useLiveData

// start_example
internal class UseLiveDataComponent(
    private val viewModel: FakeLiveDataViewModel = FakeLiveDataViewModel()
) : KComponent() {

  override fun ComponentScope.render(): Component? {
    val viewState = useLiveData(viewModel.liveData) ?: error("should not be null")

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

internal class FakeLiveDataViewModel {

  private var myCounter = 10
  private val name = "John"

  private val _liveData = MutableLiveData(ViewState(myCounter, name))
  internal val liveData: LiveData<ViewState> = _liveData

  fun onIncrementCounterClick() {
    myCounter++
    _liveData.value = ViewState(myCounter, name)
  }
}
