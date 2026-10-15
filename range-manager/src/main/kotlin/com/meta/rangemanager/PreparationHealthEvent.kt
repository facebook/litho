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

/**
 * Marker interface for typed preparation health events.
 *
 * Framework-specific modules (Litho, Compose) define sealed implementations that carry
 * statically-typed details about failures or informational signals. The core
 * [PreparationHealthTracker] stores these as-is and includes them in summary output via [toString].
 *
 * Example:
 * ```
 * sealed class LithoPreparationHealthEvent : PreparationHealthEvent {
 *     data class Late(val hasLayoutState: Boolean) : LithoPreparationHealthEvent() {
 *         override val failureType = PreparationFailureType.LATE
 *         override val details = "hasLayoutState=$hasLayoutState"
 *     }
 * }
 * ```
 */
interface PreparationHealthEvent {
  /** The type of preparation failure this event represents. */
  val failureType: PreparationFailureType

  /** Optional details about the event. */
  val details: String?
}

/** Types of preparation failures detected by the health observer. */
enum class PreparationFailureType {
  /** No preparable existed at bind time. Detected at the core level. */
  MISS,

  /** Preparation started but hadn't finished by measure time. Detected by framework extensions. */
  LATE,

  /** Preparation finished but the layout state didn't match. Detected by framework extensions. */
  WRONG,
}
