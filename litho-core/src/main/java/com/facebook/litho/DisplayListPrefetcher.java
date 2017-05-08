/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.Display;
import android.view.View;

/**
 * {@link Runnable} that is used to prefetch display lists of components for which layout has been
 * already calculated but not yet appeared on screen. This will allow for faster drawing time when
 * these components come to screen.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class DisplayListPrefetcher implements Runnable {
  private static DisplayListPrefetcher sDisplayListPrefetcher = new DisplayListPrefetcher();

  private final Queue<WeakReference<LayoutState>> mLayoutStates;

  private DisplayListPrefetcher() {
    mLayoutStates = new LinkedList<>();
  }

  public static DisplayListPrefetcher getInstance() {
    return sDisplayListPrefetcher;
  }

  synchronized void addLayoutState(LayoutState layoutState) {
    mLayoutStates.add(new WeakReference<>(layoutState));
  }

  @Override
  public void run() {
  }

  /**
   * @return the next {@link LayoutState} from the queue that has non-zero elements to process.
   */
  private LayoutState getValidLayoutStateFromQueue() {
    WeakReference<LayoutState> currentLayoutState = mLayoutStates.peek();
    while (currentLayoutState != null) {
      final LayoutState layoutState = currentLayoutState.get();
      if (layoutState == null || !layoutState.hasItemsForDLPrefetch()) {
        mLayoutStates.remove();
        currentLayoutState = mLayoutStates.peek();
      } else {
        break;
      }
    }

    if (currentLayoutState == null) {
      // Queue is empty.
      return null;
    }

    return currentLayoutState.get();
  }
}
