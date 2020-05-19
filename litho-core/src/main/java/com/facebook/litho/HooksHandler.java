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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Holds information about the hooks of the components in a Component Tree. */
public class HooksHandler {

  private static final int INITIAL_HOOKS_CONTAINER_CAPACITY = 4;

  /** Maps a component key to a hook list for that component. */
  private Map<String, Hooks> mHooksContainer;

  public HooksHandler() {
    this(null);
  }

  public HooksHandler(@Nullable HooksHandler hooksHandler) {
    if (hooksHandler != null) {
      synchronized (this) {
        copyHooksFrom(hooksHandler);
      }
    } else {
      mHooksContainer = new HashMap<>(INITIAL_HOOKS_CONTAINER_CAPACITY);
    }
  }

  Hooks getOrCreate(String key) {
    Hooks hooks = mHooksContainer.get(key);
    if (hooks == null) {
      hooks = new Hooks();
      mHooksContainer.put(key, hooks);
    }
    return hooks;
  }

  /**
   * Called when creating a new HooksHandler for a layout calculation. It copies the source of truth
   * hooks. These blocks are run immediately to update this hook before we start creating
   * components.
   *
   * @param other the ComponentTree's source-of-truth HooksHandler
   */
  private void copyHooksFrom(HooksHandler other) {
    mHooksContainer = new HashMap<>(other.mHooksContainer.size());

    for (Map.Entry<String, Hooks> entry : other.mHooksContainer.entrySet()) {
      mHooksContainer.put(entry.getKey(), new Hooks(entry.getValue()));
    }
  }

  /**
   * Called on the ComponentTree's source-of-truth HooksHandler when a layout has completed and new
   * hooks need to be committed. We copy over the new mapping from hook keys to values.
   *
   * @param hooksHandler the HooksHandler whose layout is being committed
   */
  void commit(HooksHandler hooksHandler) {
    mHooksContainer.clear();

    if (hooksHandler.mHooksContainer != null && !hooksHandler.mHooksContainer.isEmpty()) {
      mHooksContainer.putAll(hooksHandler.mHooksContainer);
    }
  }

  @VisibleForTesting
  public Map<String, Hooks> getHooksContainer() {
    return Collections.unmodifiableMap(mHooksContainer);
  }
}
