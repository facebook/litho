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

import com.facebook.litho.PerfEvent

/**
 * The Litho logging system is designed to be stateless and have batches of data sent to the server
 * for processing, to avoid overhead on the device. This sample implementation is to illustrate the
 * data you can work with, not on how to actually implement logging on the client.
 */
object PerfEventStore {
  private val events: MutableMap<PerfEvent, EventData> = hashMapOf()

  fun obtain(eventId: Int, instanceKey: Int): PerfEvent {
    val event = SamplePerfEvent(eventId, instanceKey)
    events[event] = EventData(System.nanoTime())
    return event
  }

  fun markerAnnotate(event: SamplePerfEvent, annotationKey: String, annotationValue: Any?) {
    // We can be pretty lose with lookup here as we control event creation.
    // Also, if we misuse the APIs in the framework, we don't want that to silently fail.
    events[event]!!.addAnnotation(annotationKey, annotationValue)
  }

  fun markerPoint(event: SamplePerfEvent, eventName: String) {
    events[event]!!.addMarker(System.nanoTime(), eventName)
  }

  fun release(event: PerfEvent): EventData {
    val data = events.remove(event)!!
    data.endTimeNs = System.nanoTime()
    return data
  }
}
