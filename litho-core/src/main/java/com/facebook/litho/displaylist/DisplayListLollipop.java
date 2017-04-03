/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.displaylist;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.view.HardwareCanvas;
import android.view.RenderNode;
import android.view.View;

import static com.facebook.litho.displaylist.Utils.safeInvoke;

/**
 * Implementation of {@link PlatformDisplayList} for Android Lollipop.
 */
public class DisplayListLollipop implements PlatformDisplayList {

  private final RenderNode mDisplayList;

  static PlatformDisplayList createDisplayList(String debugName) {
    RenderNode renderNode = RenderNode.create(debugName, null);
    return new DisplayListLollipop(renderNode);
  }

  private DisplayListLollipop(RenderNode displayList) {
    mDisplayList = displayList;
  }

  @Override
  public Canvas start(int width, int height) {
    Object canvas = mDisplayList.start(width, height);
    return ((Canvas) canvas);
  }

  @Override
  public void end(Canvas canvas) {
    Object hardwareCanvas = canvas;
    mDisplayList.end((HardwareCanvas) hardwareCanvas);
  }

  @Override
  public void clear() {
    mDisplayList.destroyDisplayListData();
  }

  @Override
  public void print(Canvas canvas) {
    mDisplayList.output();
  }

  @Override
  public void draw(Canvas canvas) throws DisplayListException {
    Object hardwareCanvas = canvas;
    if (!(hardwareCanvas instanceof HardwareCanvas)) {
      throw new DisplayListException(new ClassCastException());
    }

    ((HardwareCanvas) hardwareCanvas).drawRenderNode(mDisplayList);
  }

  @Override
  public void setBounds(int left, int top, int right, int bottom) {
    mDisplayList.setLeftTopRightBottom(left, top, right, bottom);
    mDisplayList.setClipToBounds(false);
  }

  @Override
  public boolean isValid() {
    return mDisplayList.isValid();
  }
}
