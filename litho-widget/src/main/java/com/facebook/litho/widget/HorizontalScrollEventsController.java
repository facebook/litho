/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.annotation.Nullable;

/**
 * A controller that can be passed as {@link com.facebook.litho.annotations.Prop} to a {@link
 * HorizontalScrollSpec} to trigger events from outside the component hierarchy.
 */
public class HorizontalScrollEventsController {

  private @Nullable HorizontalScrollSpec.HorizontalScrollLithoView mHorizontalScrollView;

  void setScrollableView(@Nullable HorizontalScrollSpec.HorizontalScrollLithoView scrollableView) {
    mHorizontalScrollView = scrollableView;
  }

  public void scrollTo(int x) {
    if (mHorizontalScrollView != null) {
      mHorizontalScrollView.scrollTo(x, 0);
    }
  }

  public void smoothScrollTo(int x) {
    if (mHorizontalScrollView != null) {
      mHorizontalScrollView.smoothScrollTo(x, 0);
    }
  }
}
