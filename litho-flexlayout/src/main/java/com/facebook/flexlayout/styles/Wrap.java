// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

public enum Wrap {
  NO_WRAP(0),
  WRAP(1),
  WRAP_REVERSE(2);

  private final int mIntValue;

  Wrap(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static Wrap fromInt(int value) {
    switch (value) {
      case 0:
        return NO_WRAP;
      case 1:
        return WRAP;
      case 2:
        return WRAP_REVERSE;
      default:
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
