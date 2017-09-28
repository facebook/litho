/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import android.content.Context;
import com.facebook.litho.widget.LinearLayoutInfo;

/**
 * A Factory used to create {@link LinearLayoutInfo}s in {@link ListRecyclerConfiguration}.
 */
public interface LinearLayoutInfoFactory {
  /**
   * @return a new {@link LinearLayoutInfo} that will be used to compute the layouts of the children of
   * the {@link ListRecyclerConfiguration}.
   */
  LinearLayoutInfo createLinearLayoutInfo(Context context, int orientation, boolean reverseLayout);
}
