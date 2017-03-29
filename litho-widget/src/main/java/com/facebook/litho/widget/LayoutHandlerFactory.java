/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import javax.annotation.Nullable;

import com.facebook.litho.ComponentInfo;
import com.facebook.litho.LayoutHandler;

/**
 * A Factory used to create {@link LayoutHandler}s in {@link RecyclerBinder}.
 */
public interface LayoutHandlerFactory {
  /**
   * @return a new {@link LayoutHandler} that will be used to compute the layouts of the children of
   * the {@link RecyclerSpec}.
   */
  @Nullable
  LayoutHandler createLayoutCalculationHandler(ComponentInfo componentInfo);
}
