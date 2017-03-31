/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package android.view;

public class RenderNode {

  public static final int FLAG_CLIP_CHILDREN = 0x1;

  public static RenderNode create(String name, View owningView) {
    throw new RuntimeException("Stub!");
  }

  public static RenderNode adopt(long nativePtr) {
    throw new RuntimeException("Stub!");
  }

  public HardwareCanvas start(int width, int height) {
    throw new RuntimeException("Stub!");
  }

  public void end(HardwareCanvas endCanvas) {
    throw new RuntimeException("Stub!");
  }

  public void end(DisplayListCanvas endCanvas) {
    throw new RuntimeException("Stub!");
  }

  public void destroyDisplayListData() {
    throw new RuntimeException("Stub!");
  }

  public void discardDisplayList() {
    throw new RuntimeException("Stub!");
  }

  public boolean setLeftTopRightBottom(int left, int top, int right, int bottom) {
    throw new RuntimeException("Stub!");
  }

  public void output() {
    throw new RuntimeException("Stub!");
  }

  public boolean isValid() {
    throw new RuntimeException("Stub!");
  }

  public boolean setClipToBounds(boolean clipToBounds) {
    throw new RuntimeException("Stub!");
  }
}
