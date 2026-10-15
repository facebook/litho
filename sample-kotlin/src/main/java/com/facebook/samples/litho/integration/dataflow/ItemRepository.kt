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

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import kotlinx.coroutines.delay

/**
 * A fake repository simulating a network data source. In a real app, this would call a GraphQL
 * endpoint, REST API, or local database.
 */
// start_example
@DataClassGenerate data class ItemModel(val id: Int, val name: String, val description: String)

class ItemRepository {

  suspend fun fetchItems(): Result<List<ItemModel>> {
    delay(1500) // simulate network delay
    return if (kotlin.random.Random.nextFloat() > 0.2f) {
      Result.success(
          listOf(
              ItemModel(1, "Reels", "Short-form video content"),
              ItemModel(2, "Stories", "24-hour ephemeral content"),
              ItemModel(3, "Feed", "Personalized content feed"),
              ItemModel(4, "Direct", "Private messaging"),
          ),
      )
    } else {
      Result.failure(Exception("Network error"))
    }
  }
}
// end_example
