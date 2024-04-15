// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

public enum Edge {
  LEFT(0),
  TOP(1),
  RIGHT(2),
  BOTTOM(3);

  private final int mIntValue;

  Edge(int intValue) {
    mIntValue = intValue;
  }

  public int intValue() {
    return mIntValue;
  }

  public static Edge fromInt(int value) {
    switch (value) {
      case 0:
        return LEFT;
      case 1:
        return TOP;
      case 2:
        return RIGHT;
      case 3:
        return BOTTOM;
      default:
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
  }
}
