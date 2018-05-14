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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TestPerfEvent implements PerfEvent {

  private static final AtomicInteger sInstanceCounter = new AtomicInteger(0);
  private final int mMarkerId;
  private final Map<String, Object> mAnnotations = new HashMap<>();
  private final List<String> mPoints = new LinkedList<>();

  public TestPerfEvent(int markerId) {
    mMarkerId = markerId;
  }

  /**
   * Identify a particular instance of a performance event if there are multiple, parallel events of
   * the same marker id.
   */
  @Override
  public int getInstanceKey() {
    return sInstanceCounter.getAndIncrement();
  }

  /** Identify the type of a performance event. */
  @Override
  public int getMarkerId() {
    return mMarkerId;
  }

  /**
   * Adds a key:value annotation to an already active marker.
   *
   * @param annotationKey
   * @param annotationValue
   */
  @Override
  public void markerAnnotate(String annotationKey, String annotationValue) {
    mAnnotations.put(annotationKey, annotationValue);
  }

  /**
   * @param annotationKey
   * @param annotationValue
   * @see #markerAnnotate(String, String)
   */
  @Override
  public void markerAnnotate(String annotationKey, double annotationValue) {
    mAnnotations.put(annotationKey, annotationValue);
  }

  /**
   * @param annotationKey
   * @param annotationValue
   * @see #markerAnnotate(String, String)
   */
  @Override
  public void markerAnnotate(String annotationKey, int annotationValue) {
    mAnnotations.put(annotationKey, annotationValue);
  }

  /**
   * @param annotationKey
   * @param annotationValue
   * @see #markerAnnotate(String, String)
   */
  @Override
  public void markerAnnotate(String annotationKey, boolean annotationValue) {
    mAnnotations.put(annotationKey, annotationValue);
  }

  /**
   * @param annotationKey
   * @param annotationValue
   * @see #markerAnnotate(String, String)
   */
  @Override
  public void markerAnnotate(String annotationKey, String[] annotationValue) {
    mAnnotations.put(annotationKey, annotationValue);
  }

  /**
   * @param annotationKey
   * @param annotationValue
   * @see #markerAnnotate(String, String)
   */
  @Override
  public void markerAnnotate(String annotationKey, Double[] annotationValue) {
    mAnnotations.put(annotationKey, annotationValue);
  }

  /**
   * @param annotationKey
   * @param annotationValue
   * @see #markerAnnotate(String, String)
   */
  @Override
  public void markerAnnotate(String annotationKey, int[] annotationValue) {
    mAnnotations.put(annotationKey, annotationValue);
  }

  /**
   * Annotate the current event with a sub-routing at the point of invocation under the given name.
   *
   * @param eventName
   */
  @Override
  public void markerPoint(String eventName) {
    mPoints.add(eventName);
  }

  public Map<String, Object> getAnnotations() {
    return mAnnotations;
  }

  public List<String> getPoints() {
    return mPoints;
  }
}
