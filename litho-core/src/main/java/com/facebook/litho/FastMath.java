/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import com.facebook.infer.annotation.Nullsafe;

/**
 * Implements some math functions in a faster way than the java Math package. This will always have
 * the downside of not supporting all the edge cases which the java Math package does support so
 * please read up on those edge cases before using these methods.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class FastMath {

  /**
   * This stack overflow post has more context around what cases this implementation won't handle.
   * http://stackoverflow.com/questions/1750739/faster-implementation-of-math-round
   *
   * @param val The value to round
   * @return The rounded value
   */
  public static int round(float val) {
    if (val > 0) {
      return (int) (val + 0.5);
    } else {
      return (int) (val - 0.5);
    }
  }
}
