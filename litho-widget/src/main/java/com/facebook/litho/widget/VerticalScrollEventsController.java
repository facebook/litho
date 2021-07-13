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
 * VerticalScrollSpec} to trigger events from outside the component hierarchy.
 */
public class VerticalScrollEventsController {

  @Nullable private LithoScrollView mScrollView;

  void setScrollView(@Nullable LithoScrollView scrollView) {
    mScrollView = scrollView;
  }

  public void scrollTo(int y) {
    if (mScrollView != null) {
      mScrollView.scrollTo(mScrollView.getScrollX(), y);
    }
  }

  public void smoothScrollTo(int y) {
    if (mScrollView != null) {
      mScrollView.smoothScrollTo(mScrollView.getScrollX(), y);
    }
  }
}
