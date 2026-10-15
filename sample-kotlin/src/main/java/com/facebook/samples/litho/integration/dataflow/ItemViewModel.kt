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

package com.facebook.samples.litho.integration.dataflow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel coordinates UseCases and exposes a single StateFlow<UiState> for the UI layer. The
 * Component subscribes to this flow via `useFlow`.
 *
 * In a real app, you would use AndroidX ViewModel with viewModelScope. This sample uses a manual
 * CoroutineScope for simplicity.
 */
// start_example
class ItemViewModel(
    private val useCase: ItemUseCase = ItemUseCase(ItemRepository()),
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val _uiState = MutableStateFlow<DataFlowUiState>(DataFlowUiState.Loading)
  val uiState: StateFlow<DataFlowUiState> = _uiState

  init {
    loadItems()
  }

  fun loadItems() {
    _uiState.value = DataFlowUiState.Loading
    scope.launch {
      try {
        val state = useCase.getItems()
        _uiState.value = state
      } catch (e: Exception) {
        _uiState.value =
            DataFlowUiState.Error(message = e.message ?: "Unknown error", isRetryable = true)
      }
    }
  }

  fun onDestroy() {
    scope.cancel()
  }
}
// end_example
