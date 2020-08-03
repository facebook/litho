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

package com.facebook.litho.widget.events;

public class EventWithoutAnnotation {
  public int count;
  public boolean isDirty;
  public String message;

  public EventWithoutAnnotation() {}

  public EventWithoutAnnotation(int count, boolean isDirty, String message) {
    this.count = count;
    this.isDirty = isDirty;
    this.message = message;
  }
}
