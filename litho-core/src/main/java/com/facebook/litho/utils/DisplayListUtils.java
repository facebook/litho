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

package com.facebook.litho.utils;

import android.os.Build;
import android.view.View;
import com.facebook.litho.DisplayListPrefetcher;

/**
 * Provides static methods related to display list generation.
 */
public class DisplayListUtils {

  public static void prefetchDisplayLists(View view) {
    final DisplayListPrefetcher displayListPrefetcher = DisplayListPrefetcher.getInstance();

    if (displayListPrefetcher.hasPrefetchItems()) {
      displayListPrefetcher.setHostingView(view);
      view.post(displayListPrefetcher);
    }
  }

  public static boolean isEligibleForCreatingDisplayLists() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }
}
