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

package com.facebook.litho

import java.util.HashSet

/**
 * Logger for tracking Litho events happening during startup.
 *
 * The implementations decide how to log points (via [LithoStartupLogger.markPoint]) and whether
 * this logger is enabled (via [LithoStartupLogger.isEnabled]).
 */
abstract class LithoStartupLogger {

  companion object {
    const val LITHO_PREFIX = "litho"
    const val CHANGESET_CALCULATION = "_changeset"
    const val FIRST_LAYOUT = "_firstlayout"
    const val FIRST_MOUNT = "_firstmount"
    const val LAST_MOUNT = "_lastmount"
    const val START = "_start"
    const val END = "_end"

    private val NEEDS_THREAD_INFO =
        HashSet<String>().apply {
          add(CHANGESET_CALCULATION)
          add(FIRST_LAYOUT)
        }

    @JvmStatic
    fun isEnabled(logger: LithoStartupLogger?): Boolean = logger != null && logger.isEnabled
  }

  /**
   * @return attribution to the rendered events like the network query name, data source
   *   (network/cache) etc.
   */
  var latestDataAttribution: String = ""
    private set

  private val processedEvents = HashSet<String>()
  private val startedEvents = HashSet<String>()

  private fun markPoint(name: String) {
    if (!processedEvents.contains(name)) {
      onMarkPoint(name)
      processedEvents.add(name)
    }
  }

  private fun getFullMarkerName(eventName: String, dataAttribution: String, stage: String): String =
      buildString {
        append(LITHO_PREFIX)
        if (NEEDS_THREAD_INFO.contains(eventName)) {
          append(if (ThreadUtils.isMainThread()) "_ui" else "_bg")
        }
        if (dataAttribution.isNotEmpty()) {
          append('_')
          append(dataAttribution)
        }
        append(eventName)
        append(stage)
      }

  /**
   * Set attribution to the rendered events like the network query name, data source (network/cache)
   * etc.
   */
  fun setDataAttribution(attribution: String) {
    latestDataAttribution = attribution
  }

  /** Mark the event with given name, stage (start/end), and given data attribution. */
  /**
   * Mark the event with given name and stage (start/end). It will use currently assigned data
   * attribution.
   */
  @JvmOverloads
  fun markPoint(eventName: String, stage: String, dataAttribution: String = latestDataAttribution) {
    if (stage == START) {
      startedEvents.add(getFullMarkerName(eventName, dataAttribution, ""))
    } else if (stage == END &&
        !startedEvents.remove(getFullMarkerName(eventName, dataAttribution, ""))) {
      // no matching start point, skip (can happen for changeset end)
      return
    }
    markPoint(getFullMarkerName(eventName, dataAttribution, stage))
  }

  /** Callback to log the event point. */
  protected abstract fun onMarkPoint(name: String)

  /** @return whether this logger is active. */
  protected abstract val isEnabled: Boolean
}
