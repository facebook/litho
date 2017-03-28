/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.yoga;

/**
 * Helpers for building measure output value.
 */
public class YogaMeasureOutput {

  public static long make(float width, float height) {
    final int wBits = Float.floatToRawIntBits(width);
    final int hBits = Float.floatToRawIntBits(height);
    return ((long) wBits) << 32 | ((long) hBits);
  }

  public static long make(int width, int height) {
    return make((float) width, (float) height);
  }

  public static float getWidth(long measureOutput) {
    return Float.intBitsToFloat((int) (0xFFFFFFFF & (measureOutput >> 32)));
  }

  public static float getHeight(long measureOutput) {
    return Float.intBitsToFloat((int) (0xFFFFFFFF & measureOutput));
  }
}
