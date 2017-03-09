// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import javax.annotation.Nullable;

import com.facebook.components.ComponentInfo;
import com.facebook.components.LayoutHandler;

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
