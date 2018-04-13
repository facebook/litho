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

import static com.facebook.litho.displaylist.Utils.safeInvoke;

import android.graphics.Canvas;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import java.lang.reflect.Method;

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
