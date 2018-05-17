/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.TreeProp;
import java.util.Collections;
import java.util.Map;

/**
 * A data structure to store tree props.
 * @see TreeProp
 */
@ThreadConfined(ThreadConfined.ANY)
public class TreeProps {

  private final Map<Class, Object> mMap = Collections.synchronizedMap(new ArrayMap<Class, Object>());

  public void put(Class key, Object value) {
    mMap.put(key, value);
  }

  @Nullable
  public <T> T get(Class<T> key) {
    return (T) mMap.get(key);
  }

  public Map<Class, Object> toMap() {
    return Collections.unmodifiableMap(mMap);
  }

  /**
   * Whenever a Spec sets tree props, the TreeProps map from the parent is copied.
   *
   * Infer knows that newProps is owned but doesn't know that newProps.mMap is owned.
   */
  @ThreadSafe(enableChecks = false)
  public static TreeProps copy(TreeProps source) {
    final TreeProps newProps = ComponentsPools.acquireTreeProps();
    if (source != null) {
      newProps.mMap.putAll((Map<? extends Class, ?>) source.mMap);
    }

    return newProps;
  }

  void reset() {
    mMap.clear();
  }
}
