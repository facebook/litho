// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.v4.util.SimpleArrayMap;

import com.facebook.components.annotations.TreeProp;
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
