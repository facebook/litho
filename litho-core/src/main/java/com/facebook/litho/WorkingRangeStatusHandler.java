/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SimpleArrayMap;

/** A handler that stores the range status of components with given working range. */
public class WorkingRangeStatusHandler {

  @IntDef({STATUS_UNINITIALIZED, STATUS_IN_RANGE, STATUS_OUT_OF_RANGE})
  public @interface WorkingRangeStatus {}

  static final int STATUS_UNINITIALIZED = 0;
  static final int STATUS_IN_RANGE = 1;
  static final int STATUS_OUT_OF_RANGE = 2;

  /**
   * Use a {@link SimpleArrayMap} to store status of components with given working range. The key of
   * the container is combined with the component's global key and the working range name.
   *
   * <p>The global key guarantees the uniqueness of the component in a ComponentTree, and it's
   * consistent across different {@link LayoutState}s. The working range name is used to find the
   * specific working range since a component can have several working ranges. The value is an
   * integer presenting the status.
   */
  private final SimpleArrayMap<String, Integer> mStatus = new SimpleArrayMap<>();

  boolean isInRange(String name, Component component) {
    return getStatus(name, component) == STATUS_IN_RANGE;
  }

  /** Components in the collection share same status, we can only check the first component. */
  @WorkingRangeStatus
  private int getStatus(String name, Component component) {
    final String key = generateKey(name, component.getGlobalKey());
    if (mStatus.containsKey(key)) {
      return mStatus.get(key);
    }

    return STATUS_UNINITIALIZED;
  }

  void setEnteredRangeStatus(String name, Component component) {
    setStatus(name, component, STATUS_IN_RANGE);
  }

  void setExitedRangeStatus(String name, Component component) {
    setStatus(name, component, STATUS_OUT_OF_RANGE);
  }

  void clear() {
    mStatus.clear();
  }

  @VisibleForTesting
  SimpleArrayMap<String, Integer> getStatus() {
    return mStatus;
  }

  @VisibleForTesting
  void setStatus(String name, Component component, @WorkingRangeStatus int status) {
    final String globalKey = component.getGlobalKey();
    mStatus.put(generateKey(name, globalKey), status);
  }

  private static String generateKey(String name, String globalKey) {
    return name + "_" + globalKey;
  }
}
