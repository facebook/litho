// Copyright 2004-present Facebook. All Rights Reserved.

package android.view;

public abstract class DisplayList {
  public static final int FLAG_CLIP_CHILDREN = 0x1;

  public abstract HardwareCanvas start();

  public abstract HardwareCanvas start(int width, int height);

  public abstract void end();

  public abstract void clear();

  public abstract int getSize();

  public abstract void setLeftTopRightBottom(int left, int top, int right, int bottom);

  public abstract boolean isValid();

  public abstract void invalidate();

  public abstract void setClipChildren(boolean clipChildren);

  public abstract void setClipToBounds(boolean clipToBounds);
}
