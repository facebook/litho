/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.utils;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.components.ComponentView;

import static com.facebook.components.ThreadUtils.assertMainThread;

/**
 * Provides methods for enabling incremental mount.
 */
public class IncrementalMountUtils {

  /**
   * A view that wraps a child view and that provides a wrapped view to be incrementally mounted.
   */
  public interface WrapperView {

    /**
     * @return A child view that will be incrementally mounted.
     */
    View getWrappedView();
  }

  private static final Rect sTmpRect = new Rect();

  /**
   * Performs incremental mount on the children views of the given ViewGroup.
   * @param scrollingViewParent ViewGroup container of views that will be incrementally mounted.
   */
  public static void performIncrementalMount(ViewGroup scrollingViewParent) {
    assertMainThread();
    final int viewGroupWidth = scrollingViewParent.getWidth();
