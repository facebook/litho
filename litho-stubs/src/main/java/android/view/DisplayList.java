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
