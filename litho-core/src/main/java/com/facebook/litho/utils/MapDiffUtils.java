/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.utils;

import androidx.annotation.Nullable;
import com.facebook.litho.CommonUtils;
import java.util.Map;

public final class MapDiffUtils {

  /** Return whether the two maps have the same keys and values. */
  public static <K, V> boolean areMapsEqual(@Nullable Map<K, V> prev, @Nullable Map<K, V> next) {
    if (prev == next) {
      return true;
    }

    if (prev == null || next == null) {
      return false;
    }

    if (prev.size() != next.size()) {
      return false;
    }

    for (Map.Entry<K, V> entry : prev.entrySet()) {
      if (!CommonUtils.equals(entry.getValue(), next.get(entry.getKey()))) {
        return false;
      }
    }

    return true;
  }
}
