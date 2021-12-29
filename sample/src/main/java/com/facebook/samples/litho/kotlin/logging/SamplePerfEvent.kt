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

data class SamplePerfEvent(private val markerId: Int, private val instanceKey: Int) : PerfEvent {

  override fun getInstanceKey(): Int = instanceKey

  override fun getMarkerId(): Int = markerId

  override fun markerAnnotate(annotationKey: String, annotationValue: String?) {
    PerfEventStore.markerAnnotate(this, annotationKey, annotationValue)
  }

  override fun markerAnnotate(annotationKey: String, annotationValue: Double) {
    PerfEventStore.markerAnnotate(this, annotationKey, annotationValue)
  }

  override fun markerAnnotate(annotationKey: String, annotationValue: Int) {
    PerfEventStore.markerAnnotate(this, annotationKey, annotationValue)
  }

  override fun markerAnnotate(annotationKey: String, annotationValue: Boolean) {
    PerfEventStore.markerAnnotate(this, annotationKey, annotationValue)
  }

  override fun markerAnnotate(annotationKey: String, annotationValue: Array<out String>) {
    // Using Lists here only to make formatting easier. Otherwise we'd have to
    // keep separate stores for the types (which would be the right thing to do).
    PerfEventStore.markerAnnotate(this, annotationKey, annotationValue.toList())
  }

  override fun markerAnnotate(annotationKey: String, annotationValue: Array<out Double>) {
    PerfEventStore.markerAnnotate(this, annotationKey, annotationValue.toList())
  }

  override fun markerAnnotate(annotationKey: String, annotationValue: IntArray) {
    PerfEventStore.markerAnnotate(this, annotationKey, annotationValue.toList())
  }

  override fun markerPoint(eventName: String) {
    PerfEventStore.markerPoint(this, eventName)
  }
}
