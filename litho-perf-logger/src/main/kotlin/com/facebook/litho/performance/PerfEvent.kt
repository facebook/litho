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

/**
 * A logging event created through [ComponentsLogger.newPerformanceEvent] to track performance
 * metrics in the framework.
 */
interface PerfEvent {

  /**
   * Identify a particular instance of a performance event if there are multiple, parallel events of
   * the same marker id.
   */
  val instanceKey: Int

  /** Identify the type of a performance event. */
  val markerId: Int

  /** Adds a key:value annotation to an already active marker. */
  fun markerAnnotate(annotationKey: String, annotationValue: String?)

  /** @see .markerAnnotate */
  fun markerAnnotate(annotationKey: String, annotationValue: Double)

  /** @see .markerAnnotate */
  fun markerAnnotate(annotationKey: String, annotationValue: Int)

  /** @see .markerAnnotate */
  fun markerAnnotate(annotationKey: String, annotationValue: Boolean)

  /** @see .markerAnnotate */
  fun markerAnnotate(annotationKey: String, annotationValue: Array<String>)

  /** @see .markerAnnotate */
  fun markerAnnotate(annotationKey: String, annotationValue: Array<Double>)

  /** @see .markerAnnotate */
  fun markerAnnotate(annotationKey: String, annotationValue: IntArray)

  /**
   * Annotate the current event with a sub-routing at the point of invocation under the given name.
   */
  fun markerPoint(eventName: String)
}
