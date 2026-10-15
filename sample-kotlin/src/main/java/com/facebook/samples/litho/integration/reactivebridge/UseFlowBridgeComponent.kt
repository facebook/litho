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

package com.facebook.samples.litho.integration.reactivebridge

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCached
import com.facebook.litho.useFlow
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Demonstrates `useFlow` -- the preferred way to bridge Kotlin StateFlow/Flow into Litho state.
 *
 * When the flow emits a new value, the component automatically re-renders. This is the recommended
 * pattern for all new Litho code that consumes reactive data.
 */
// start_example
class UseFlowBridgeComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val repository = useCached { FakeCounterRepository() }
    val count = useFlow(repository.counter)

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "useFlow Bridge (preferred pattern)",
              textSize = 18.sp,
              style = Style.padding(bottom = 8.dp),
          ),
      )
      child(
          Text(
              text = "The component subscribes to a StateFlow via useFlow.",
              textSize = 14.sp,
              style = Style.padding(bottom = 12.dp),
          ),
      )
      child(Text(text = "Count: $count", textSize = 24.sp, style = Style.padding(bottom = 8.dp)))
      child(
          Text(
              text = "Increment",
              textSize = 16.sp,
              style = Style.padding(all = 8.dp).onClick { repository.increment() },
          ),
      )
    }
  }
}

class FakeCounterRepository {
  private val _counter = MutableStateFlow(0)
  val counter: StateFlow<Int> = _counter

  fun increment() {
    _counter.update { it + 1 }
  }
}
// end_example
