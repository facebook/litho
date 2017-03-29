/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.utils;

import android.graphics.Rect;
import android.support.v4.util.Pools;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.ComponentView;

import static com.facebook.litho.ThreadUtils.assertMainThread;

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

