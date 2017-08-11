/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;

/**
 * A factory used to create {@link LithoView}s in {@link RecyclerBinder}.
 */
public interface LithoViewFactory {

  /**
   * @return a new {@link LithoView} that will be used to host children of the {@link RecyclerSpec}.
   */
  LithoView createLithoView(ComponentContext context);

}
