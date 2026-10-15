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

import android.util.Log

/**
 * Default implementation of [PreparationHealthObserver] that accumulates aggregate metrics for
 * preparation health.
 *
 * Surface-agnostic — any surface (Feed, Stories, Direct) can use this. Framework-specific failure
 * detection (e.g., Litho LATE/WRONG) is handled by extension events reported via [reportEvent].
 *
 * Debug-only: create only when debug config is enabled; null-check at call sites ensures zero
 * overhead in production.
 *
 * @param surfaceName Label used in summary output (e.g., "Feed", "Stories").
 */
class PreparationHealthTracker(
    private val surfaceName: String,
) : PreparationHealthObserver {

  /**
   * Represents a single preparation failure — an item that wasn't ready at the time it was needed.
   *
   * Three core failure types:
   * - **MISS**: No preparable existed at bind time. Detected at the core level.
   * - **LATE**: Preparation started but hadn't finished by measure time. Detected by framework
   *   extensions.
   * - **WRONG**: Preparation finished but the layout state didn't match. Detected by framework
   *   extensions.
   *
   * Additional failure types can be reported by framework-specific extensions via [reportEvent].
   */
  internal data class PreparationFailure(
      val type: PreparationFailureType,
      val key: String,
      val wasEvicted: Boolean = false,
      val details: String? = null,
  )

  // --- Counters ---
  private var registeredCount = 0
  private var preparedCount = 0
  private var boundCount = 0
  private var unpreparedCount = 0

  // --- Key tracking ---
  private val registeredKeys = LinkedHashSet<String>()
  private val preparedKeys = LinkedHashSet<String>()
  private val evictedKeys = mutableSetOf<String>()

  // --- Breakdown maps ---
  private val prepareSourceBreakdown = mutableMapOf<PreparationSource, Int>()
  private val unprepareReasonBreakdown = mutableMapOf<UnprepareReason, Int>()

  // --- Phase tracking ---
  private var hasFirstBind = false
  private var hasScrolled = false
  private var prepsBeforeFirstBind = 0
  private var prepsBeforeFirstScroll = 0
  private var initialLoadPrepCount = 0

  // --- Preparation failures (unified) ---
  private val mutablePreparationFailures = mutableListOf<PreparationFailure>()

  internal val preparationFailures: List<PreparationFailure>
    @Synchronized get() = mutablePreparationFailures.toList()

  // --- PreparationHealthObserver implementation ---

  @Synchronized
  override fun onItemRegistered(key: Any) {
    Log.d(TAG, "[$surfaceName] REGISTERED key=$key")
    registeredCount++
    val keyStr = key.toString()
    if (keyStr.isNotEmpty()) {
      registeredKeys.add(keyStr)
    }
  }

  @Synchronized
  override fun onItemPrepared(key: Any, source: PreparationSource) {
    Log.d(TAG, "[$surfaceName] PREPARED key=$key source=$source")
    preparedCount++
    val keyStr = key.toString()
    preparedKeys.add(keyStr)
    evictedKeys.remove(keyStr)
    prepareSourceBreakdown[source] = (prepareSourceBreakdown[source] ?: 0) + 1

    if (!hasFirstBind) {
      prepsBeforeFirstBind++
    }
    if (!hasScrolled) {
      prepsBeforeFirstScroll++
    }
  }

  @Synchronized
  override fun onItemBound(key: Any, wasPrepared: Boolean) {
    Log.d(TAG, "[$surfaceName] BOUND key=$key wasPrepared=$wasPrepared")
    boundCount++
    if (!hasFirstBind) {
      hasFirstBind = true
    }
    if (!wasPrepared) {
      val keyStr = key.toString()
      mutablePreparationFailures.add(
          PreparationFailure(
              type = PreparationFailureType.MISS,
              key = keyStr,
              wasEvicted = keyStr in evictedKeys,
          ),
      )
    }
  }

  @Synchronized
  override fun onItemUnprepared(key: Any, reason: UnprepareReason) {
    Log.d(TAG, "[$surfaceName] UNPREPARED key=$key reason=$reason")
    unpreparedCount++
    val keyStr = key.toString()
    preparedKeys.remove(keyStr)
    evictedKeys.add(keyStr)
    unprepareReasonBreakdown[reason] = (unprepareReasonBreakdown[reason] ?: 0) + 1
  }

  @Synchronized
  override fun reportEvent(key: Any, event: PreparationHealthEvent) {
    Log.d(TAG, "[$surfaceName] ${event.failureType} key=$key details=${event.details}")
    mutablePreparationFailures.add(
        PreparationFailure(
            type = event.failureType,
            key = key.toString(),
            details = event.details,
        ),
    )
  }

  @Synchronized
  override fun onFirstScroll() {
    Log.d(TAG, "[$surfaceName] FIRST_SCROLL preparedCount=$preparedCount")
    if (!hasScrolled) {
      hasScrolled = true
      initialLoadPrepCount = preparedCount
    }
  }

  // --- Summary ---

  @Synchronized
  internal fun dumpSummary(): String {
    val sb = StringBuilder()
    sb.appendLine("=== $surfaceName Preparation Health ===")
    sb.appendLine(
        "Registered: $registeredCount | Prepared: $preparedCount | Bound: $boundCount | Unprepared: $unpreparedCount",
    )
    sb.appendLine("--- Phase ---")
    sb.appendLine("Preps before first bind: $prepsBeforeFirstBind")
    sb.appendLine("Preps before first scroll: $prepsBeforeFirstScroll")
    sb.appendLine("Initial load prep count: $initialLoadPrepCount")
    sb.appendLine("Currently prepared keys: ${preparedKeys.size}")
    sb.appendLine("Currently registered keys: ${registeredKeys.size}")
    if (prepareSourceBreakdown.isNotEmpty()) {
      sb.appendLine("--- Prepare sources ---")
      prepareSourceBreakdown.forEach { (source, count) -> sb.appendLine("  $source: $count") }
    }
    if (unprepareReasonBreakdown.isNotEmpty()) {
      sb.appendLine("--- Unprepare reasons ---")
      unprepareReasonBreakdown.forEach { (reason, count) -> sb.appendLine("  $reason: $count") }
    }
    if (mutablePreparationFailures.isNotEmpty()) {
      val missCount = mutablePreparationFailures.count { it.type == PreparationFailureType.MISS }
      val lateCount = mutablePreparationFailures.count { it.type == PreparationFailureType.LATE }
      val wrongCount = mutablePreparationFailures.count { it.type == PreparationFailureType.WRONG }
      sb.appendLine(
          "--- Preparation failures (${mutablePreparationFailures.size}: $missCount MISS, $lateCount LATE, $wrongCount WRONG) ---",
      )
      mutablePreparationFailures.forEachIndexed { i, failure ->
        val evicted = if (failure.wasEvicted) "  wasEvicted=true" else ""
        val details = if (failure.details != null) "  details=${failure.details}" else ""
        sb.appendLine("  #${i + 1}  ${failure.type}  key=${failure.key}$evicted$details")
      }
    }
    return sb.toString()
  }

  companion object {
    private const val TAG = "PrepHealth"
  }
}
