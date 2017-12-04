/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;

/**
 * Keeps all valid instances of {@link EventTrigger} from the hierarchy when the layout is completed
 */
public class EventTriggersContainer {

  @Nullable private SimpleArrayMap<String, EventTrigger> mEventTriggers;

  /**
   * Record an {@link EventTrigger} according to its key.
   *
   * @param trigger
   */
  public void recordEventTrigger(@Nullable EventTrigger trigger) {
    if (trigger == null) {
      return;
    }

    if (mEventTriggers == null) {
      mEventTriggers = new SimpleArrayMap<>();
    }

    mEventTriggers.put(trigger.mKey, trigger);
  }

  /**
   * Retrieve and return an {@link EventTrigger} based on the given key.
   *
   * @param triggerKey
   * @return EventTrigger with the triggerKey given.
   */
  @Nullable
  EventTrigger getEventTrigger(String triggerKey) {
    if (mEventTriggers == null || !mEventTriggers.containsKey(triggerKey)) {
      return null;
    }

    return mEventTriggers.get(triggerKey);
  }

  void clear() {
    if (mEventTriggers != null) {
      mEventTriggers.clear();
    }
  }
}
