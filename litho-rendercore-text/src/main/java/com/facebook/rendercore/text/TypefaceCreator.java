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

package com.facebook.rendercore.text;

import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.Nullable;

public interface TypefaceCreator {
  Typeface createTypefaceFromFontFamily(Context context, String fontFamily, int style);

  @Nullable
  TypefaceBuilderWrapper createTypefaceBuilderFromFontFamily(Context context, String fontFamily);

  /**
   * Fallback typeface for variable-font spans when the requested variable font cannot produce a
   * builder (for example a remote font that has not finished downloading). The shared default is
   * the system default; apps that ship a closer bundled fallback can override this.
   */
  default Typeface getVariableFontFallbackTypeface(Context context) {
    return Typeface.DEFAULT;
  }
}
