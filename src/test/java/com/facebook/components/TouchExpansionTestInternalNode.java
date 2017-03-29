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
 * Used by the {@link com.facebook.litho.ViewNodeInfoTest}.
 */
public class TouchExpansionTestInternalNode extends InternalNode {
  @Override
  public int getX() {
    return 10;
  }

  @Override
  public int getY() {
    return 10;
  }

  @Override
  public int getHeight() {
    return 10;
  }

  @Override
  public int getWidth() {
    return 10;
  }

  @Override
  int getTouchExpansionLeft() {
    return 1;
  }

  @Override
  int getTouchExpansionTop() {
    return 2;
  }

  @Override
  int getTouchExpansionRight() {
    return 3;
  }

  @Override
  int getTouchExpansionBottom() {
    return 4;
  }

  @Override
  boolean hasTouchExpansion() {
    return true;
  }
}
