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

package com.facebook.litho.sections;

import java.util.HashSet;
import java.util.Set;

/**
 * This class manages the {@link Section}s global keys for a {@link SectionTree}. It provides
 * methods for detecting duplicate keys.
 */
public class KeyHandler {

  private final Set<String> mKnownGlobalKeys;

  public KeyHandler() {
    mKnownGlobalKeys = new HashSet<>();
  }

  public void registerKey(String globalKey) {
    mKnownGlobalKeys.add(globalKey);
  }

  /** Returns true if this KeyHandler has already recorded a component with the given key. */
  public boolean hasKey(String key) {
    return mKnownGlobalKeys.contains(key);
  }
}
