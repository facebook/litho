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

import android.util.SparseIntArray;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

final class ScopedComponentInfo {

  // Can be final if Component is stateless and cloning is not needed anymore.
  private StateContainer mStateContainer;

  /**
   * Holds onto how many direct component children of each type this Component has. Used for
   * automatically generating unique global keys for all sibling components of the same type.
   */
  @Nullable private SparseIntArray mChildCounters;

  /** Count the times a manual key is used so that clashes can be resolved. */
  @Nullable private Map<String, Integer> mManualKeysCounter;

  ScopedComponentInfo(Component component) {
    mStateContainer = component.createStateContainer();
  }

  StateContainer getStateContainer() {
    return mStateContainer;
  }

  void setStateContainer(StateContainer stateContainer) {
    mStateContainer = stateContainer;
  }

  /**
   * Returns the number of children of a given type {@param component} component has and then
   * increments it by 1.
   *
   * @param component the child component
   * @return the number of children components of type {@param component}
   */
  int getChildCountAndIncrement(final Component component) {
    if (mChildCounters == null) {
      mChildCounters = new SparseIntArray();
    }
    final int count = mChildCounters.get(component.getTypeId(), 0);
    mChildCounters.put(component.getTypeId(), count + 1);

    return count;
  }

  /**
   * Returns the number of children with same {@param manualKey} component has and then increments
   * it by 1.
   *
   * @param manualKey
   * @return
   */
  int getManualKeyUsagesCountAndIncrement(String manualKey) {
    if (mManualKeysCounter == null) {
      mManualKeysCounter = new HashMap<>();
    }
    int manualKeyIndex =
        mManualKeysCounter.containsKey(manualKey) ? mManualKeysCounter.get(manualKey) : 0;
    mManualKeysCounter.put(manualKey, manualKeyIndex + 1);

    return manualKeyIndex;
  }
}
