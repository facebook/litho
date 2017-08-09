/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import com.facebook.litho.RenderInfo;
import com.facebook.litho.LayoutHandler;
import javax.annotation.Nullable;

/**
 * A Factory used to create {@link LayoutHandler}s in {@link RecyclerBinder}.
 */
public interface LayoutHandlerFactory {
  /**
   * @return a new {@link LayoutHandler} that will be used to compute the layouts of the children of
   * the {@link RecyclerSpec}.
   */
  @Nullable
  LayoutHandler createLayoutCalculationHandler(RenderInfo renderInfo);
}
