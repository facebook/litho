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
import java.util.List;

public class ComponentKeyUtils {

  /**
   * Performs a breadth-first search on the children of the given parent component and returns the
   * global key of the first child component whose key is componentKey.
   *
   * @param parentComponent the parent of the component
   * @param componentKey the key of the component
   * @return null if no child with the given key exists or the global key of the child with that key
   */
  static @Nullable String getBestGlobalKeyForComponentKey(
      Component parentComponent, String componentKey) {
    if (parentComponent == null || componentKey == null) {
      return null;
    }

    final List<Component> children = parentComponent.getChildren();

    for (int i = 0, size = children.size(); i < size; i++) {
      final Component child = children.get(i);
      if (child == null || child.getKey() == null) {
        continue;
      }
      if (child.getKey().equals(componentKey)) {
        return parentComponent.getGlobalKey() + componentKey;
      }
    }

    for (int i = 0, size = children.size(); i < size; i++) {
      final String key = getBestGlobalKeyForComponentKey(children.get(i), componentKey);
      if (key != null) {
        return key;
      }
    }

    return null;
  }
}
