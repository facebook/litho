/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

public interface WorkingRange {

  /**
   * Return true to trigger{@literal @}OnEnteredRange method defined in the component, do nothing
   * otherwise.
   */
  boolean shouldEnterRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex);

  /**
   * Return true to trigger{@literal @}OnExitedRange method defined in the component, do nothing
   * otherwise.
   */
  boolean shouldExitRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex);
}
