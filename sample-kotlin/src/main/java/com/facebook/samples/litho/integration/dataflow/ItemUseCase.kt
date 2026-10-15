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

/**
 * UseCase transforms raw data models from the repository into UiState for the view layer. This
 * pattern keeps domain logic (filtering, mapping, validation) out of both the ViewModel and the
 * Component.
 *
 * Inspired by Instagram Android's UseCase-driven MVVM architecture where ViewModels coordinate
 * UseCases, and UseCases own the domain logic and stores.
 */
// start_example
class ItemUseCase(private val repository: ItemRepository) {

  suspend fun getItems(): DataFlowUiState {
    val result = repository.fetchItems()
    return result.fold(
        onSuccess = { models ->
          if (models.isEmpty()) {
            DataFlowUiState.Empty
          } else {
            DataFlowUiState.Loaded(items = models.map { it.toUiItem() })
          }
        },
        onFailure = { error ->
          DataFlowUiState.Error(
              message = error.message ?: "Unknown error",
              isRetryable = true,
          )
        },
    )
  }

  private fun ItemModel.toUiItem(): DataFlowUiItem =
      DataFlowUiItem(id = id, title = name, subtitle = description)
}
// end_example
