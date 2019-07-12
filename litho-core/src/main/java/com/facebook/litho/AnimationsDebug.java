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

import android.util.Log;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * Utilities for animations debug.
 */
public class AnimationsDebug {

  public static final boolean ENABLED = ComponentsConfiguration.isEndToEndTestRun;
  static final String TAG = "LithoAnimationDebug";

  static void debugPrintLayoutState(LayoutState layoutState) {
    if (!ENABLED) {
      return;
    }
    Log.d(TAG, layoutState.dumpAsString());
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
        Log.d(
            TAG,
            ""
                + i
                + " ["
                + output.getId()
                + "] ("
                + output.getTransitionId()
                + ") host => ("
                + output.getHostMarker()
                + "), locked ref count: "
                + animationLockedIndices[i]);
      }
    }
  }
}
