/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.fresco;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

/**
 * This drawable is used in place of null in DraweeDrawable. DraweeDrawable extends from
 * ForwardingDrawable which does not work with null drawables.
 */
public class NoOpDrawable extends Drawable {

  @Override
  public void draw(Canvas canvas) {
    // no op
  }

  @Override
  public int getOpacity() {
    return 0;
  }

  @Override
  public void setAlpha(int alpha) {
    // no op
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    // no op
  }
}
