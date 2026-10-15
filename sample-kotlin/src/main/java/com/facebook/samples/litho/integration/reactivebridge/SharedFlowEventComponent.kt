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

import android.util.Log
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.onCleanup
import com.facebook.litho.useCached
import com.facebook.litho.useEffect
import com.facebook.litho.useFlow
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Demonstrates using SharedFlow for one-shot events (snackbar, navigation, toast) alongside
 * StateFlow for persistent state.
 *
 * Key distinction: StateFlow holds the current state (re-emits on subscribe), while SharedFlow
 * fires one-shot events that should be consumed exactly once.
 */
// start_example
class SharedFlowEventComponent : KComponent() {

  companion object {
    private const val TAG = "SharedFlowEventComponent"
  }

  override fun ComponentScope.render(): Component {
    val viewModel = useCached { EventDemoViewModel() }
    val count = useFlow(viewModel.state)
    val lastMessage = useState { "" }

    useEffect(viewModel) {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
      scope.launch {
        try {
          viewModel.events.collect { event ->
            when (event) {
              is UiEvent.ShowMessage -> lastMessage.update(event.text)
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "Failed to collect events", e)
        }
      }
      onCleanup {
        scope.cancel()
        viewModel.onDestroy()
      }
    }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "SharedFlow Events + StateFlow State",
              textSize = 18.sp,
              style = Style.padding(bottom = 8.dp),
          ),
      )
      child(
          Text(
              text = "StateFlow for persistent state, SharedFlow for one-shot events.",
              textSize = 14.sp,
              style = Style.padding(bottom = 12.dp),
          ),
      )
      child(
          Text(
              text = "Counter: $count",
              textSize = 20.sp,
              style = Style.padding(bottom = 8.dp),
          ),
      )
      if (lastMessage.value.isNotEmpty()) {
        child(
            Text(
                text = "Message: ${lastMessage.value}",
                textSize = 14.sp,
                style = Style.padding(bottom = 8.dp),
            ),
        )
      }
      child(
          Text(
              text = "Increment (fires event at every 5th)",
              textSize = 16.sp,
              style = Style.padding(all = 8.dp).onClick { viewModel.increment() },
          ),
      )
    }
  }
}

sealed interface UiEvent {
  class ShowMessage(val text: String) : UiEvent
}

class EventDemoViewModel {
  companion object {
    private const val TAG = "EventDemoViewModel"
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val _state = MutableStateFlow(0)
  val state: StateFlow<Int> = _state

  private val _events = MutableSharedFlow<UiEvent>()
  val events: SharedFlow<UiEvent> = _events

  fun increment() {
    _state.update { it + 1 }
    val currentCount = _state.value
    if (currentCount % 5 == 0) {
      scope.launch {
        try {
          _events.emit(UiEvent.ShowMessage("Milestone: $currentCount!"))
        } catch (e: Exception) {
          Log.w(TAG, "Failed to emit event", e)
        }
      }
    }
  }

  fun onDestroy() {
    scope.cancel()
  }
}
// end_example
