/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.displaylist;

import android.view.DisplayList;
import android.view.HardwareCanvas;
import java.lang.reflect.Constructor;
import javax.annotation.Nullable;

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

  static @Nullable PlatformDisplayList createDisplayList(String debugName) {
    DisplayList displayList = instantiateDisplayList(debugName);
    if (displayList == null) {
      return null;
    }

    return new DisplayListJB(displayList);
  }

  static @Nullable DisplayList instantiateDisplayList(String debugName) {
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
  public void setTranslationX(float translationX) {
    mDisplayList.setTranslationX(translationX);
  }

  @Override
  public void setTranslationY(float translationY) {
    mDisplayList.setTranslationY(translationY);
  }

  @Override
  public boolean isValid() {
    return mDisplayList.isValid();
  }
}
