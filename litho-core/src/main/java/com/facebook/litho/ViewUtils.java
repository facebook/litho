/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.drawable.ComparableColorDrawable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ViewUtils {

  static void setViewForeground(View view, @Nullable Drawable foreground) {
    if (foreground != null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        throw new IllegalStateException(
            "MountState has a ViewNodeInfo with foreground however "
                + "the current Android version doesn't support foreground on Views");
      }

      view.setForeground(foreground);
    }
  }

  static void setViewForeground(View view, int foregroundColor) {
    final Drawable foreground = ComparableColorDrawable.create(foregroundColor);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new IllegalStateException(
          "MountState has a ViewNodeInfo with foreground however "
              + "the current Android version doesn't support foreground on Views");
    }

    view.setForeground(foreground);
  }
}
