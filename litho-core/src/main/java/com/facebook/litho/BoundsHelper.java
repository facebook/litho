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

package com.facebook.litho;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class BoundsHelper {

  public static void applySizeToDrawableForAnimation(Drawable drawable, int width, int height) {
    final Rect bounds = drawable.getBounds();
    drawable.setBounds(bounds.left, bounds.top, bounds.left + width, bounds.top + height);

    if (drawable instanceof MatrixDrawable) {
      ((MatrixDrawable) drawable).bind(width, height);
    }
  }

  public static void applyXYToDrawableForAnimation(Drawable drawable, int x, int y) {
    final Rect bounds = drawable.getBounds();
    drawable.setBounds(x, y, bounds.width() + x, bounds.height() + y);
  }
}
