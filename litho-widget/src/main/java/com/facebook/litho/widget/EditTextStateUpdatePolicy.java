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

package com.facebook.litho.widget;

/**
 * EditTextStateUpdatePolicy specifies when EditText component should update its internal input
 * state
 *
 * @example use UPDATE_ON_LINE_COUNT_CHANGE to create an EditText components that has a static
 *     width, but should dynamically expand in height as user types in (WRAP_CONTENT effect)
 */
public enum EditTextStateUpdatePolicy {
  /** No updates are performed */
  NO_UPDATES,
  /**
   * {@code lazyUpdateInput()} is called every time text changes, thus allowing to preserve input
   * state during layout recalculations
   */
  ONLY_LAZY_UPDATES,
  /**
   * Expands ONLY_LAZY_UPDATES by calling {@code updateInput()} when line count changes, which
   * triggers re-layout
   */
  UPDATE_ON_LINE_COUNT_CHANGE,
  /**
   * {@code updateInput()} is called every time text changes, thus every change leads to
   * recalculating layout
   */
  UPDATE_ON_TEXT_CHANGE
}
