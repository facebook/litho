/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonUtils {

  private CommonUtils() {}

  /** Polyfill of Objects.hash that can be used on API<19. */
  public static int hash(Object... values) {
    return Arrays.hashCode(values);
  }

  static final @Nullable <T> List<T> mergeLists(@Nullable List<T> a, @Nullable List<T> b) {
    if (a == null || a.isEmpty()) {
      return b;
    }
    if (b == null || b.isEmpty()) {
      return a;
    }
    ArrayList<T> result = new ArrayList<>(a.size() + b.size());
    result.addAll(a);
    result.addAll(b);
    return result;
  }
}
