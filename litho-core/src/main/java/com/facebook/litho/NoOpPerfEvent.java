/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import com.facebook.infer.annotation.Nullsafe;

/** Empty implementation of {@link PerfEvent} performing no actions. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NoOpPerfEvent implements PerfEvent {
  @Override
  public int getInstanceKey() {
    return 0;
  }

  @Override
  public int getMarkerId() {
    return 0;
  }

  @Override
  public void markerAnnotate(String annotationKey, String annotationValue) {}

  @Override
  public void markerAnnotate(String annotationKey, int annotationValue) {}

  @Override
  public void markerAnnotate(String annotationKey, boolean annotationValue) {}

  @Override
  public void markerAnnotate(String annotationKey, String[] annotationValue) {}

  @Override
  public void markerAnnotate(String annotationKey, Double[] annotationValue) {}

  @Override
  public void markerAnnotate(String annotationKey, int[] annotationValue) {}

  @Override
  public void markerPoint(String eventName) {}

  @Override
  public void markerAnnotate(String annotationKey, double annotationValue) {}
}
