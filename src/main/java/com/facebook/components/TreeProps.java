/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.util.SimpleArrayMap;

import com.facebook.litho.annotations.TreeProp;
import com.facebook.infer.annotation.ThreadConfined;

/**
 * A data structure to store tree props.
 * @see {@link TreeProp}.
 */
@ThreadConfined(ThreadConfined.ANY)
public class TreeProps {

  private final SimpleArrayMap<Class, Object> mMap = new SimpleArrayMap<>();

  public void put(Class key, Object value) {
    mMap.put(key, value);
  }

  public <T> T get(Class key) {
    return (T) mMap.get(key);
  }

  /**
   * Whenever a Spec sets tree props, the TreeProps map from the parent is copied.
   */
  public static TreeProps copy(TreeProps source) {
    final TreeProps newProps = ComponentsPools.acquireTreeProps();
    if (source != null) {
      newProps.mMap.putAll(source.mMap);
    }

    return newProps;
  }

  void reset() {
    mMap.clear();
  }
}
