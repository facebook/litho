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
