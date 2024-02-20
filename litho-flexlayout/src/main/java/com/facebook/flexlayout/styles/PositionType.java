// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

public enum PositionType {
  RELATIVE(0),
  ABSOLUTE(1);

  private final int mIntValue;

  PositionType(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static PositionType fromInt(int value) {
    switch (value) {
      case 0:
        return RELATIVE;
      case 1:
        return ABSOLUTE;
      default:
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
