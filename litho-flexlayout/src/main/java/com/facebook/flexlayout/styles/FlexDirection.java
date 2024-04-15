// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

public enum FlexDirection {
  ROW(0),
  ROW_REVERSE(1),
  COLUMN(2),
  COLUMN_REVERSE(3);

  private final int mIntValue;

  FlexDirection(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static FlexDirection fromInt(int value) {
    switch (value) {
      case 0:
        return ROW;
      case 1:
        return ROW_REVERSE;
      case 2:
        return COLUMN;
      case 3:
        return COLUMN_REVERSE;
      default:
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
