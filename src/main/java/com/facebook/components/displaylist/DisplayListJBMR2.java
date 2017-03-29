/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.displaylist;

import android.app.Activity;
import android.graphics.Canvas;
import android.view.DisplayList;
import android.view.HardwareCanvas;

/**
 * Implementation of {@link PlatformDisplayList} for Android JellyBeans MR2 and KitKat.
 */
class DisplayListJBMR2 extends DisplayListJB {

  DisplayListJBMR2(android.view.DisplayList displayList) {
    super(displayList);
  }

  static PlatformDisplayList createDisplayList(String debugName) {
    final DisplayList displayList = instantiateDisplayList(debugName);

    if (displayList == null) {
      return null;
    }

    return new DisplayListJBMR2(displayList);
  }

  @Override
  public Canvas start(int width, int height) {
    final Object canvas = mDisplayList.start(width, height);

    return (Canvas) canvas;
  }

  @Override
  public void end(android.graphics.Canvas canvas) {
    mDisplayList.end();
  }

  @Override
  public void setBounds(int left, int top, int right, int bottom) {
    mDisplayList.setLeftTopRightBottom(left, top, right, bottom);
    mDisplayList.setClipToBounds(false);
  }

  @Override
  public void clear() {
    mDisplayList.clear();
  }
}
