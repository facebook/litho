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

package com.facebook.litho.rangemanager

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.meta.rangemanager.PreparationFailureType
import com.meta.rangemanager.PreparationHealthEvent

/**
 * Litho-specific preparation health events detected via [DebugEventBus].
 *
 * These events are reported to a [PreparationHealthObserver] via [reportEvent] by a
 * [LithoHealthEventSource] that subscribes to Litho's debug event infrastructure.
 */
sealed class LithoPreparationHealthEvent : PreparationHealthEvent {

  /**
   * Async layout was started but hadn't finished by measure time. The ComponentTree exists but has
   * no layout state yet — the main thread had to do a synchronous layout.
   */
  data object Late : LithoPreparationHealthEvent() {
    override val failureType: PreparationFailureType = PreparationFailureType.LATE
    override val details: String = "NO_LAYOUT_STATE"
  }

  /**
   * Work that ran on the main thread outside measure. [source] is Litho's render source; [phase] is
   * which part of the render main did: all of it, only the layout because the tree was already
   * resolved, or the rest of a background resolve it interrupted. [requestedBy] is the component
   * whose synchronous state update caused it, when one did.
   */
  @DataClassGenerate
  data class MainThreadRender(
      val source: String,
      val phase: Phase = Phase.RESOLVE,
      val requestedBy: String? = null,
  ) : LithoPreparationHealthEvent() {
    override val failureType: PreparationFailureType = PreparationFailureType.WRONG
    private val cause: String = if (requestedBy == null) source else "$source, $requestedBy"
    override val details: String =
        when (phase) {
          Phase.RESOLVE -> "MAIN_THREAD_RENDER($cause)"
          Phase.LAYOUT -> "MAIN_THREAD_LAYOUT($cause)"
          Phase.RESUME -> "MAIN_THREAD_RESUME($cause)"
        }

    enum class Phase {
      RESOLVE,
      LAYOUT,
      RESUME,
    }
  }

  /**
   * Layout state existed but didn't match at measure time. Either the root component ID changed or
   * the size specs didn't match — the main thread had to re-resolve.
   */
  @DataClassGenerate
  data class Wrong(
      val sizeSpecsMatch: Boolean,
      val idMatch: Boolean,
      val currentSizeConstraint: String? = null,
      val incomingSizeConstraint: String? = null,
  ) : LithoPreparationHealthEvent() {
    override val failureType: PreparationFailureType = PreparationFailureType.WRONG
    override val details: String =
        when {
          !idMatch -> "ID_MISMATCH"
          !sizeSpecsMatch -> {
            val specs = listOfNotNull(
                currentSizeConstraint?.let { "current=[$it]" },
                incomingSizeConstraint?.let { "incoming=[$it]" },
            )
                .joinToString(", ")
            if (specs.isNotEmpty()) "SIZE_SPEC_MISMATCH($specs)" else "SIZE_SPEC_MISMATCH"
          }
          else -> "UNKNOWN"
        }
  }
}
