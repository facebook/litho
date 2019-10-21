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

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps all valid instances of {@link EventTrigger} from the hierarchy when the layout is completed
 */
public class EventTriggersContainer {

  @GuardedBy("this")
  @Nullable
  private Map<String, EventTrigger> mEventTriggers;

  /**
   * Record an {@link EventTrigger} according to its key.
   *
   * @param trigger
   */
  public void recordEventTrigger(@Nullable EventTrigger trigger) {
    if (trigger == null) {
      return;
    }

    synchronized (this) {
      if (mEventTriggers == null) {
        mEventTriggers = new HashMap<>();
      }

      mEventTriggers.put(trigger.mKey, trigger);
    }
  }

  /**
   * Retrieve and return an {@link EventTrigger} based on the given key.
   *
   * @param triggerKey
   * @return EventTrigger with the triggerKey given.
   */
  @Nullable
  public synchronized EventTrigger getEventTrigger(String triggerKey) {
    if (mEventTriggers == null || !mEventTriggers.containsKey(triggerKey)) {
      return null;
    }

    return mEventTriggers.get(triggerKey);
  }

  public synchronized void clear() {
    if (mEventTriggers != null) {
      mEventTriggers.clear();
    }
  }
}
