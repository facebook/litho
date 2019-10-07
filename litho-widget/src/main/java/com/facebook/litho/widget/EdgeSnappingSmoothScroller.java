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

import android.content.Context;
import androidx.recyclerview.widget.LinearSmoothScroller;

/** LinearSmoothScroller subclass that snaps the target position to start/end or either ends. */
public class EdgeSnappingSmoothScroller extends LinearSmoothScroller {

  private final int mSnapPreference;
  private final int mOffset;

  public EdgeSnappingSmoothScroller(Context context, int snapPreference, int offset) {
    super(context);
    mSnapPreference = snapPreference;
    mOffset = offset;
  }

  @Override
  public int calculateDtToFit(
      int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
    int result = super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference);
    return result + mOffset;
  }

  @Override
  protected int getVerticalSnapPreference() {
    return mSnapPreference;
  }

  @Override
  protected int getHorizontalSnapPreference() {
    return mSnapPreference;
  }
}
