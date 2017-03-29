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
public enum YogaAlign {
  AUTO(0),
  FLEX_START(1),
  CENTER(2),
  FLEX_END(3),
  STRETCH(4),
  BASELINE(5),
  SPACE_BETWEEN(6),
  SPACE_AROUND(7);

  private int mIntValue;

  YogaAlign(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static YogaAlign fromInt(int value) {
    switch (value) {
      case 0: return AUTO;
      case 1: return FLEX_START;
      case 2: return CENTER;
      case 3: return FLEX_END;
      case 4: return STRETCH;
      case 5: return BASELINE;
      case 6: return SPACE_BETWEEN;
      case 7: return SPACE_AROUND;
      default: throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
