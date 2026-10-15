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

package com.facebook.samples.litho.integration.uistate

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate

/**
 * Sealed UiState pattern inspired by Instagram Android's MVVM architecture.
 *
 * Each screen defines its own sealed UiState with variants for each visual state. Components
 * receive this as a prop and use exhaustive `when` to render the appropriate UI.
 */
// start_example
sealed interface ScreenUiState {
  data object Loading : ScreenUiState

  @DataClassGenerate data class Loaded(val items: List<Item>) : ScreenUiState

  @DataClassGenerate data class Error(val message: String, val isRetryable: Boolean) : ScreenUiState

  data object Empty : ScreenUiState
}

@DataClassGenerate data class Item(val id: Int, val title: String, val subtitle: String)
// end_example
