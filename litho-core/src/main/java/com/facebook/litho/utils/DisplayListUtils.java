/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
