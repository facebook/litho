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
package com.facebook.litho;

import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CommonUtils {

  private CommonUtils() {}

  /** @return {@code true} iff a and b are equal. */
  public static boolean equals(@Nullable Object a, @Nullable Object b) {
    if (a == b) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    return a.equals(b);
  }

  /**
   * Adds an item to a possibly null list to defer the allocation as long as possible.
   *
   * @param list the nullable list.
   * @param item the item to add.
   */
  public static <A> List<A> addOrCreateList(@Nullable List<A> list, A item) {
    if (list == null) {
      list = new LinkedList<>();
    }

    list.add(item);

    return list;
  }

  /** Polyfill of Objects.hash that can be used on API<19. */
  public static int hash(Object... values) {
    return Arrays.hashCode(values);
  }
}
