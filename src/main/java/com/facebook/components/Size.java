// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

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
