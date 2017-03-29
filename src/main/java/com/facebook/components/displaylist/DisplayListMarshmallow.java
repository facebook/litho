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
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import android.view.View;

import static com.facebook.litho.displaylist.Utils.safeInvoke;

/**
 * Implementation of {@link PlatformDisplayList} for Android Marshmallow.
 */
public class DisplayListMarshmallow implements PlatformDisplayList {

  private static Class sRenderNodeClass;

  private static Method sStartMethod;

  protected static boolean sInitialized = false;
  protected static boolean sInitializationFailed = false;
  protected final RenderNode mDisplayList;

  protected static void ensureInitialized() throws Exception {
    if (sInitialized || sInitializationFailed) {
      return;
    }

    // Unfortunately we still need to use reflection here since in Marshmallow Google changed
    // the return of this method from HardwareCanvas to DisplayListCanvas.
    sRenderNodeClass = Class.forName("android.view.RenderNode");

    sStartMethod = sRenderNodeClass.getDeclaredMethod("start", int.class, int.class);
    sInitialized = true;
  }

  static PlatformDisplayList createDisplayList(String debugName) {
    try {
      ensureInitialized();
      if (sInitialized) {
        RenderNode renderNode = RenderNode.create(debugName, null);
        return new DisplayListMarshmallow(renderNode);
      }
    } catch (Throwable e) {
      sInitializationFailed = true;
    }

    return null;
  }

  DisplayListMarshmallow(RenderNode displayList) {
    mDisplayList = displayList;
  }

  @Override
  public Canvas start(int width, int height) throws DisplayListException {
    return (Canvas) safeInvoke(sStartMethod, mDisplayList, width, height);
  }

  @Override
  public void end(Canvas canvas) {
    Object displayListCanvas = canvas;
    mDisplayList.end((DisplayListCanvas) displayListCanvas);
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
    Object displayListCanvas = canvas;
    if (!(displayListCanvas instanceof DisplayListCanvas)) {
      throw new DisplayListException(new ClassCastException());
    }

    ((DisplayListCanvas) displayListCanvas).drawRenderNode(mDisplayList);
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
