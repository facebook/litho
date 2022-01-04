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

package com.facebook.litho.testing.assertj;

import java.util.List;
import org.assertj.core.api.Condition;
import org.assertj.core.description.TextDescription;

public class Conditions {

  public static <L extends List<T>, T> Condition<L> exactly(final T item, final int times) {
    return new Condition<L>(new TextDescription("exactly %d %s", times, item)) {
      int count;

      @Override
      public boolean matches(L value) {
        for (T element : value) {
          if (item.equals(element)) {
            count++;
          }
          if (count > times) {
            return false;
          }
        }
        return count == times;
      }
    };
  }
}
