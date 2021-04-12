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

import com.facebook.infer.annotation.Nullsafe;

/**
 * Manages a Litho ComponentTree lifecycle and informs subscribed LithoLifecycleListeners when a
 * lifecycle state occurs.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface LithoLifecycleProvider {
  enum LithoLifecycle {
    HINT_VISIBLE("HINT_VISIBLE"),
    HINT_INVISIBLE("HINT_INVISIBLE"),
    DESTROYED("DESTROYED");

    private final String name;

    LithoLifecycle(String s) {
      name = s;
    }

    public boolean equalsName(String otherName) {
      return name.equals(otherName);
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  void moveToLifecycle(LithoLifecycle lithoLifecycle);

  LithoLifecycle getLifecycleStatus();

  void addListener(LithoLifecycleListener listener);

  void removeListener(LithoLifecycleListener listener);
}
