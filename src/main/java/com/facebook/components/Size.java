/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.infer.annotation.ThreadConfined;

/**
 * Public API for MeasureOutput.
 */
@ThreadConfined(ThreadConfined.ANY)
public class Size {

  public int width;
  public int height;

  public Size() {
    this.width = 0;
    this.height = 0;
  }

  public Size(int width, int height) {
    this.width = width;
    this.height = height;
  }

}
