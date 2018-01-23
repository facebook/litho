/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
