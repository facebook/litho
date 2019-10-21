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

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.collection.SimpleArrayMap;
import java.util.HashMap;
import java.util.Map;

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
  private final Map<String, Integer> mStatus = new HashMap<>();

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
  Map<String, Integer> getStatus() {
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
