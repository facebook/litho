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
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentsLogger
import com.facebook.litho.FrameworkLogEvents
import com.facebook.litho.PerfEvent
import com.facebook.litho.TreeProps
import java.util.concurrent.atomic.AtomicInteger

class SampleComponentsLogger : ComponentsLogger {
  private val tag = "LITHOSAMPLE"
  private val instanceCounter: AtomicInteger = AtomicInteger(0)

  override fun newPerformanceEvent(c: ComponentContext, eventId: Int): PerfEvent {
    return PerfEventStore.obtain(eventId, instanceCounter.incrementAndGet())
  }

  override fun logPerfEvent(event: PerfEvent) {
    printEventData(event, PerfEventStore.release(event))
  }

  override fun cancelPerfEvent(event: PerfEvent) {
    PerfEventStore.release(event)
  }

  override fun getExtraAnnotations(treeProps: TreeProps): MutableMap<String, String> =
      treeProps?.get(LogContext::class.java)?.let { mutableMapOf("log_context" to "$it") }
          ?: mutableMapOf()

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
        FrameworkLogEvents.EVENT_MOUNT -> "MOUNT"
        FrameworkLogEvents.EVENT_PRE_ALLOCATE_MOUNT_CONTENT -> "PRE_ALLOCATE_MOUNT_CONTENT"
        FrameworkLogEvents.EVENT_SECTIONS_CREATE_NEW_TREE -> "SECTIONS_CREATE_NEW_TREE"
        FrameworkLogEvents.EVENT_SECTIONS_DATA_DIFF_CALCULATE_DIFF ->
            "SECTIONS_DATA_DIFF_CALCULATE_DIFF"
        FrameworkLogEvents.EVENT_SECTIONS_GENERATE_CHANGESET -> "SECTIONS_GENERATE_CHANGESET"
        FrameworkLogEvents.EVENT_SECTIONS_ON_CREATE_CHILDREN -> "SECTIONS_ON_CREATE_CHILDREN"
        FrameworkLogEvents.EVENT_SECTIONS_SET_ROOT -> "SECTIONS_SET_ROOT"
        FrameworkLogEvents.EVENT_CALCULATE_LAYOUT_STATE -> "CALCULATE_LAYOUT_STATE"
        FrameworkLogEvents.EVENT_BENCHMARK_RUN -> "EVENT_BENCHMARK_RUN"
        FrameworkLogEvents.EVENT_RESUME_CALCULATE_LAYOUT_STATE ->
            "EVENT_RESUME_CALCULATE_LAYOUT_STATE"
        FrameworkLogEvents.EVENT_INIT_RANGE -> "EVENT_INIT_RANGE"
        FrameworkLogEvents.EVENT_LAYOUT_STATE_FUTURE_GET_WAIT ->
            "EVENT_LAYOUT_STATE_FUTURE_GET_WAIT"
        else -> "UNKNOWN"
      }

  private fun Float.withDigits(digits: Int) = "%.${digits}f".format(this)
}
