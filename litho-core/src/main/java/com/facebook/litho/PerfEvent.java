/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

/**
 * A logging event created through {@link ComponentsLogger#newPerformanceEvent(ComponentContext,
 * int)} to track performance metrics in the framework.
 */
public interface PerfEvent {

  /**
   * Identify a particular instance of a performance event if there are multiple, parallel events of
   * the same marker id.
   */
  int getInstanceKey();

  /** Identify the type of a performance event. */
  int getMarkerId();

  /** Adds a key:value annotation to an already active marker. */
  void markerAnnotate(String annotationKey, String annotationValue);

  /** @see #markerAnnotate(String, String) */
  void markerAnnotate(String annotationKey, double annotationValue);

  /** @see #markerAnnotate(String, String) */
  void markerAnnotate(String annotationKey, int annotationValue);

  /** @see #markerAnnotate(String, String) */
  void markerAnnotate(String annotationKey, boolean annotationValue);

  /** @see #markerAnnotate(String, String) */
  void markerAnnotate(String annotationKey, String[] annotationValue);

  /** @see #markerAnnotate(String, String) */
  void markerAnnotate(String annotationKey, Double[] annotationValue);

  /** @see #markerAnnotate(String, String) */
  void markerAnnotate(String annotationKey, int[] annotationValue);

  /**
   * Annotate the current event with a sub-routing at the point of invocation under the given name.
   */
  void markerPoint(String eventName);
}
