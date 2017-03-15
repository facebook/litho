// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

/**
 * Specification for the expected input to this node. This is used when initializing a graph to
 * propagate initial values throughout newly added nodes.
 */
public final class InputSpec {

  public static final long UNSPECIFIED = InputSpec.create(false, Float.NaN);

  private static final int NEEDS_SPECIFIC_VALUE = 1 << 0;

  public static final long create(boolean needsSpecificValue, float value) {
    final int valueAsInt = Float.floatToIntBits(value);
    int flags = 0;
    if (needsSpecificValue) {
      flags |= NEEDS_SPECIFIC_VALUE;
    }
    final long valueAsLong = ((long) valueAsInt) & 0xffffffffL;
    final long flagsAsLong = (((long) flags) & 0xffffffffL) << 32;
    return valueAsLong | flagsAsLong;
  }

  public static float getValue(long inputSpec) {
    final int valueAsInt = (int) inputSpec;
    return Float.intBitsToFloat(valueAsInt);
  }

  public static boolean getNeedsSpecificValue(long inputSpec) {
    final int flagsAsInt = (int) (inputSpec >> 32);
    return (flagsAsInt & NEEDS_SPECIFIC_VALUE) != 0;
  }

  private InputSpec() {
  }
}
