/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
