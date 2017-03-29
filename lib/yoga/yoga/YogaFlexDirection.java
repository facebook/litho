/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.yoga;

import com.facebook.proguard.annotations.DoNotStrip;

@DoNotStrip
public enum YogaFlexDirection {
  COLUMN(0),
  COLUMN_REVERSE(1),
  ROW(2),
  ROW_REVERSE(3);

  private int mIntValue;

  YogaFlexDirection(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static YogaFlexDirection fromInt(int value) {
    switch (value) {
      case 0: return COLUMN;
      case 1: return COLUMN_REVERSE;
      case 2: return ROW;
      case 3: return ROW_REVERSE;
      default: throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
