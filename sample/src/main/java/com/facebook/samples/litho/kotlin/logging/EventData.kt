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

data class EventData(val startTimeNs: Long) {
  private val annotations: MutableMap<String, Any?> = hashMapOf()
  private val markers: MutableList<Pair<Long, String>> = mutableListOf()

  var endTimeNs: Long? = null

  fun addAnnotation(annotationKey: String, annotationValue: Any?): EventData = apply {
    annotations[annotationKey] = annotationValue
  }

  fun getAnnotations(): Map<String, Any?> = annotations

  fun addMarker(timeNs: Long, eventName: String): EventData = apply {
    markers.add(Pair(timeNs, eventName))
  }

  fun getMarkers(): List<Pair<Long, String>> = markers
}
