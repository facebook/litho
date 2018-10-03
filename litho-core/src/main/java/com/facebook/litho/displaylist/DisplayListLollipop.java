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

import android.graphics.Canvas;
import android.view.HardwareCanvas;
import android.view.RenderNode;

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
