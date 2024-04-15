// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

public enum Overflow {
  VISIBLE(0),
  HIDDEN(1),
  SCROLL(2);

  private final int mIntValue;

  Overflow(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static Overflow fromInt(int value) {
    switch (value) {
      case 0:
        return VISIBLE;
      case 1:
        return HIDDEN;
      case 2:
        return SCROLL;
      default:
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
