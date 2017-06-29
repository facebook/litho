/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.util.Log;

/**
 * Utilities for animations debug.
 */
public class AnimationsDebug {

  static final boolean ENABLED = false;
  static final String TAG = "LithoAnimationDebug";

  static void debugPrintLayoutState(LayoutState layoutState) {
    if (!ENABLED) {
      return;
    }

    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      final LayoutOutput output = layoutState.getMountableOutputAt(i);
      final ViewNodeInfo viewNodeInfo = output.getViewNodeInfo();
      final String key = (viewNodeInfo != null) ? viewNodeInfo.getTransitionKey() : null;

      Log.d(
          TAG,
          "" + i + " [" + output.getId() + "] (" + key + ") host => (" + output.getHostMarker() +
              ")");
    }
  }

  static void debugPrintAnimationLockedIndices(
      LayoutState layoutState,
      int[] animationLockedIndices) {
    if (!ENABLED) {
      return;
    }

    if (animationLockedIndices != null) {
      for (int i = 0; i < animationLockedIndices.length; i++) {
        final LayoutOutput output = layoutState.getMountableOutputAt(i);
        final String key = output.getTransitionKey();

        Log.d(
            TAG,
            "" + i + " [" + output.getId() + "] (" + key + ") host => (" + output.getHostMarker() +
                "), locked ref count: " + animationLockedIndices[i]);
      }
    }
  }
}
