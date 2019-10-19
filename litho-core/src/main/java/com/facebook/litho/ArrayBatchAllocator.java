/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

public class ArrayBatchAllocator {

  private static int batchSize = 200;

  @Nullable private static int[][] arrays = null;
  private static int index = 0;

  /** same as calling new int[2]; */
  public static int[] newArrayOfSize2() {
    if (arrays == null || arrays.length == index) {
      arrays = new int[batchSize][2];
      index = 0;
    }
    int[] toReturn = arrays[index];
    arrays[index++] = null;
    return toReturn;
  }
}
