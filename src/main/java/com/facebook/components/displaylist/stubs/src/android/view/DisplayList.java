/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

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
