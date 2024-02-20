// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

public enum Display {
  FLEX(0),
  NONE(1);

  private final int mIntValue;

  Display(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static Display fromInt(int value) {
    switch (value) {
      case 0:
        return FLEX;
      case 1:
        return NONE;
      default:
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
