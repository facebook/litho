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

public class ArrayBatchAllocator {

  private static final int batchSize = 200;

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
