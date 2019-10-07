/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import androidx.annotation.Nullable;

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
