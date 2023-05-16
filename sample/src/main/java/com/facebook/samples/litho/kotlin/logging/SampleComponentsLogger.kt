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

package com.facebook.samples.litho.kotlin.logging

import android.util.Log
import com.facebook.litho.ComponentsLogger
import com.facebook.litho.FrameworkLogEvents
import com.facebook.litho.PerfEvent
import com.facebook.litho.TreePropertyProvider
import java.util.concurrent.atomic.AtomicInteger

class SampleComponentsLogger : ComponentsLogger {
  private val tag = "LITHOSAMPLE"
  private val instanceCounter: AtomicInteger = AtomicInteger(0)

  override fun newPerformanceEvent(eventId: Int): PerfEvent {
    return PerfEventStore.obtain(eventId, instanceCounter.incrementAndGet())
  }

  override fun logPerfEvent(event: PerfEvent) {
    printEventData(event, PerfEventStore.release(event))
  }

  override fun cancelPerfEvent(event: PerfEvent) {
    PerfEventStore.release(event)
  }

  override fun getExtraAnnotations(
      treePropertyProvider: TreePropertyProvider
  ): Map<String, String> {
    val logContext = treePropertyProvider.getProperty(LogContext::class.java)
    return buildMap { logContext?.let { put("log_context", "$it") } }
  }

  override fun isTracing(logEvent: PerfEvent): Boolean = true

  private fun printEventData(event: PerfEvent, data: EventData) {
    val totalTimeMs = (data.endTimeNs!! - data.startTimeNs) / (1000 * 1000f)
    var msg =
        """
        |--- <PERFEVENT> ---
        |type: ${getEventNameById(event.markerId)}
        |total time: ${totalTimeMs.withDigits(2)}ms
        """

    val annotations = formatAnnotations(data)
    if (annotations.isNotEmpty()) {
      msg += "|annotations: $annotations\n"
    }

    val markers = formatMarkers(data)
    if (markers.isNotEmpty()) {
      msg += "|markers: $markers\n"
    }

    msg += "|--- </PERFEVENT> ---\n"

    Log.v(tag, msg.trimMargin())
  }

  private fun formatMarkers(data: EventData): String =
      data.getMarkers().joinToString {
        "${((it.first - data.startTimeNs) / (1000 * 1000f)).withDigits(2)}ms -> ${it.second}"
      }

  private fun formatAnnotations(data: EventData): String =
      data.getAnnotations().map { "${it.key}=${it.value}" }.joinToString()

  private fun getEventNameById(@FrameworkLogEvents.LogEventId markerId: Int): String =
      when (markerId) {
        FrameworkLogEvents.EVENT_PRE_ALLOCATE_MOUNT_CONTENT -> "PRE_ALLOCATE_MOUNT_CONTENT"
        FrameworkLogEvents.EVENT_CALCULATE_RESOLVE -> "RESOLVE"
        FrameworkLogEvents.EVENT_INIT_RANGE -> "EVENT_INIT_RANGE"
        FrameworkLogEvents.EVENT_COMPONENT_RESOLVE -> "EVENT_COMPONENT_RESOLVE"
        FrameworkLogEvents.EVENT_COMPONENT_PREPARE -> "EVENT_COMPONENT_PREPARE"
        else -> "UNKNOWN"
      }

  private fun Float.withDigits(digits: Int) = "%.${digits}f".format(this)
}
