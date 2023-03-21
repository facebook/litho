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

/** Empty implementation of [PerfEvent] performing no actions. */
class NoOpPerfEvent : PerfEvent {

  override val instanceKey: Int = 0

  override val markerId: Int = 0

  override fun markerAnnotate(annotationKey: String, annotationValue: String?) = Unit

  override fun markerAnnotate(annotationKey: String, annotationValue: Int) = Unit

  override fun markerAnnotate(annotationKey: String, annotationValue: Boolean) = Unit

  override fun markerAnnotate(annotationKey: String, annotationValue: Array<String>) = Unit

  override fun markerAnnotate(annotationKey: String, annotationValue: Array<Double>) = Unit

  override fun markerAnnotate(annotationKey: String, annotationValue: IntArray) = Unit

  override fun markerPoint(eventName: String) = Unit

  override fun markerAnnotate(annotationKey: String, annotationValue: Double) = Unit
}
