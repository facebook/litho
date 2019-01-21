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
package com.facebook.litho;

import android.graphics.drawable.Drawable;
import android.os.Build;

public class AlphaHelper {

  public static float getAlpha(Drawable drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      final int scaledValue = drawable.getAlpha();
      return scaledValue / 255f;
    } else {
      // TODO(T28432326): using reflection check if drawable has getAlpha(), if so, invoke it
      return 1;
    }
  }

  public static void setAlpha(Drawable drawable, float value) {
    final int scaledValue = (int) (value * 255);
    drawable.setAlpha(scaledValue);
  }
}
