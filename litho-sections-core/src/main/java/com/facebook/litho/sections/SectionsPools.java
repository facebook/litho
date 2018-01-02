/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import static com.facebook.litho.sections.SectionLifecycle.StateUpdate;

import android.support.v4.util.ArrayMap;
import com.facebook.litho.RecyclePool;
import java.util.List;
import java.util.Map;

/** Pools of recycled resources. */
public class SectionsPools {

  private static final RecyclePool<Map<String, List<StateUpdate>>> sStateUpdatesMapPool =
      new RecyclePool<>("", 4, false);

  static Map<String, List<StateUpdate>> acquireStateUpdatesMap() {
    Map<String, List<StateUpdate>> map = sStateUpdatesMapPool.acquire();
    if (map == null) {
      map = new ArrayMap<>();
    }

    return map;
  }

  static void release(Map<String, List<StateUpdate>> map) {
    map.clear();
    sStateUpdatesMapPool.release(map);
  }
}
