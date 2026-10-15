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

package com.meta.rangemanager

/** Describes what triggered an item's preparation. */
enum class PreparationSource {
  /** Item was prepared by the range worker during a scheduled range update. */
  RANGE_SCHEDULE,

  /** Item was prepared explicitly via [KeyPreparingRangeWorker.prepareKey]. */
  PREPARE_KEY,

  /**
   * Item was prepared via [KeyPreparingRangeWorker.prepareKeyIfInRange] because it was in range.
   */
  PREPARE_KEY_IN_RANGE,
}
