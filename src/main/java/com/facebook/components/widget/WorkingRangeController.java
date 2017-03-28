/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

/**
 * The RangeController used to calculate the range for which the ComponentTrees will be created. A
 * reference to this type of controller is stored in {@link Binder}. This class will be extended
 * by all the specific RangeControllers that deal with computing the range for a particular layout.
 */
public abstract class WorkingRangeController {

  private BaseBinder mBinder;

  /**
   * Sets the {@link Binder} that the range controller is associated with.
   */
  public void setBinder(BaseBinder binder) {
    mBinder = binder;
  }

  /**
   * Returns the {@link Binder} corresponding to this range controller instance.
   */
  public BaseBinder getBinder() {
    return mBinder;
  }

  /**
   * Updates the working range for the underlying binder.
   * @param start the position of the first item included in the range
   * @param count the number of items included in the range
   */
  public void updateWorkingRange(int start, int count) {
    updateWorkingRange(start, count, BaseBinder.URFLAG_RELEASE_OUTSIDE_RANGE);
  }

  /**
   * Updates the working range for the underlying binder.
   * @param start the position of the first item included in the range
   * @param count the number of items included in the range
   * @param flags special options for the action executed on the range
   */
  public void updateWorkingRange(int start, int count, int flags) {
    // Update the range according to the newly computed position.
    mBinder.updateRange(
        start,
        count,
        flags);
  }
}
