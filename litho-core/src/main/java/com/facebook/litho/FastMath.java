/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * Implements some math functions in a faster way than the java Math package.
 * This will always have the downside of not supporting all the edge cases which the java Math
 * package does support so please read up on those edge cases before using these methods.
 */
class FastMath {

  /**
   * This stack overflow post has more context around what cases this implementation won't handle.
   * http://stackoverflow.com/questions/1750739/faster-implementation-of-math-round
   *
   * @param val The value to round
   * @return The rounded value
   */
  static int round(float val) {
    if (val > 0) {
      return (int) (val + 0.5);
    } else {
      return (int) (val - 0.5);
    }
  }
}
