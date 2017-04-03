/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.displaylist;

import java.lang.reflect.Constructor;

import android.view.DisplayList;
import android.view.HardwareCanvas;

/**
 * Implementation of {@link PlatformDisplayList} for Android JellyBeans.
 */
class DisplayListJB implements PlatformDisplayList {

  private static Class sDisplayListClass;

  private static boolean sInitialized = false;
  private static boolean sInitializationFailed = false;
  private static Constructor sDisplayListConstructor;

  protected final android.view.DisplayList mDisplayList;

  private static void ensureInitialized() throws Exception {
    if (sInitialized || sInitializationFailed) {
      return;
    }

    sDisplayListClass = Class.forName("android.view.GLES20DisplayList");
    sDisplayListConstructor = sDisplayListClass.getDeclaredConstructor(String.class);
    sDisplayListConstructor.setAccessible(true);

    sInitialized = true;
  }

  static PlatformDisplayList createDisplayList(String debugName) {
    DisplayList displayList = instantiateDisplayList(debugName);
    if (displayList == null) {
      return null;
    }

    return new DisplayListJB(displayList);
  }

  static DisplayList instantiateDisplayList(String debugName) {
    try {
      ensureInitialized();

      if (! sInitialized) {
        return null;
      }
    } catch (Throwable e) {
      sInitializationFailed = true;
    }

    try {
      return (DisplayList) sDisplayListConstructor.newInstance(debugName);
    } catch (Throwable e) {
      // Nothing to do here.
    }

    return null;
  }

  DisplayListJB(android.view.DisplayList displayList) {
    mDisplayList = displayList;
  }

  @Override
  public android.graphics.Canvas start(int width, int height) {
    final Object canvas = mDisplayList.start();
    ((HardwareCanvas) canvas).setViewport(width, height);
    ((HardwareCanvas) canvas).onPreDraw(null);
    return (android.graphics.Canvas) canvas;
  }

  @Override
  public void end(android.graphics.Canvas canvas) {
    final Object hardwareCanvas = canvas;
    ((HardwareCanvas) hardwareCanvas).onPostDraw();
    mDisplayList.end();
  }

  @Override
  public void clear() {
    mDisplayList.invalidate();
    mDisplayList.clear();
  }

  @Override
  public void print(android.graphics.Canvas canvas) throws DisplayListException {
    // NOT SUPPORTED
  }

  @Override
  public void draw(android.graphics.Canvas canvas) throws DisplayListException {
    final Object hardwareCanvas = canvas;
    if (!(hardwareCanvas instanceof HardwareCanvas)) {
      throw new DisplayListException(new ClassCastException());
    }

    ((HardwareCanvas) hardwareCanvas).drawDisplayList(
        mDisplayList,
        null,
        0);
  }

  @Override
  public void setBounds(int left, int top, int right, int bottom) {
    mDisplayList.setLeftTopRightBottom(left, top, right, bottom);
    mDisplayList.setClipChildren(false);
  }

  @Override
  public boolean isValid() {
    return mDisplayList.isValid();
  }
}
