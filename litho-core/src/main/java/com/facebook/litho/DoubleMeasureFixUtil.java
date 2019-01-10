/*
 * Copyright 2018-present Facebook, Inc.
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

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class DoubleMeasureFixUtil {

  /**
   * Correction for an Android bug on some devices with "special" densities where the system will
   * double-measure with slightly different widths in the same traversal. See
   * https://issuetracker.google.com/issues/73804230 for more details.
   *
   * <p>This hits Litho extra-hard because we create {@link LayoutState}s in measure in many places,
   * so we try to correct for it here by replacing the widthSpec given to us from above with what we
   * think the correct one is. Even though the double measure will still happen, the incorrect width
   * will not propagate to any vertical RecyclerViews contained within.
   */
  public static int correctWidthSpecForAndroidDoubleMeasureBug(Resources resources, int widthSpec) {
    final @SizeSpec.MeasureSpecMode int mode = SizeSpec.getMode(widthSpec);
    if (mode == SizeSpec.UNSPECIFIED) {
      return widthSpec;
    }

    final Configuration configuration = resources.getConfiguration();
    final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
    final int screenWidthPx = displayMetrics.widthPixels;
    final float screenDensity = displayMetrics.density;

    // NB: Logic taken from ViewRootImpl#dipToPx
    final int calculatedScreenWidthPx = (int) (screenDensity * configuration.screenWidthDp + 0.5f);

    if (screenWidthPx != calculatedScreenWidthPx
        && calculatedScreenWidthPx == SizeSpec.getSize(widthSpec)) {
      return SizeSpec.makeSizeSpec(screenWidthPx, mode);
    }

    return widthSpec;
  }
}
