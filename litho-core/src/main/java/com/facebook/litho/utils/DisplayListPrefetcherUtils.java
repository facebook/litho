/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.utils;

import android.view.View;

import com.facebook.litho.DisplayListPrefetcher;

/**
 * Provides static methods to initiate display list generation.
 */
public class DisplayListPrefetcherUtils {

  public static void prefetchDisplayLists(View view) {
    final DisplayListPrefetcher displayListPrefetcher = DisplayListPrefetcher.getInstance();

    if (displayListPrefetcher.hasPrefetchItems()) {
      displayListPrefetcher.setHostingView(view);
      view.post(displayListPrefetcher);
    }
  }
}
