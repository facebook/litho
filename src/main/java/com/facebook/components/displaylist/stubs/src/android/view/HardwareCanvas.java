/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package android.view;

import android.graphics.Rect;

public abstract class HardwareCanvas {
  public void drawRenderNode(RenderNode renderNode) {
    throw new RuntimeException("Stub!");
  }

  public abstract int drawDisplayList(DisplayList displayList, Rect dirty, int flags);

  public void setViewport(int width, int height) {
    throw new RuntimeException("Stub!");
  }

  public abstract int onPreDraw(Rect dirty);

  public abstract void onPostDraw();
}
