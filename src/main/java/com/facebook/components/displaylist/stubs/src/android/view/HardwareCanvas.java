// Copyright 2004-present Facebook. All Rights Reserved.

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
