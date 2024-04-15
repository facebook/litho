// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

public enum Direction {
  INHERIT(0),
  LTR(1),
  RTL(2);

  private final int mIntValue;

  Direction(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static Direction fromInt(int value) {
    switch (value) {
      case 0:
        return INHERIT;
      case 1:
        return LTR;
      case 2:
        return RTL;
      default:
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
