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
import android.view.DisplayList;
import javax.annotation.Nullable;

/**
 * Implementation of {@link PlatformDisplayList} for Android JellyBeans MR2 and KitKat.
 */
class DisplayListJBMR2 extends DisplayListJB {

  DisplayListJBMR2(android.view.DisplayList displayList) {
    super(displayList);
  }

  static @Nullable PlatformDisplayList createDisplayList(String debugName) {
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
