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

package com.facebook.litho;

import androidx.annotation.Nullable;

public class LayoutResultHolder {

  final LithoLayoutResult mResult;
  final @Nullable InternalNode mPartiallyResolvedLayout;

  public LayoutResultHolder(final LithoLayoutResult result) {
    mResult = result;
    mPartiallyResolvedLayout = null;
  }

  private LayoutResultHolder(final InternalNode partiallyResolvedLayout) {
    mResult = null;
    mPartiallyResolvedLayout = partiallyResolvedLayout;
  }

  public boolean wasLayoutInterrupted() {
    return mPartiallyResolvedLayout != null;
  }

  public static LayoutResultHolder interrupted(final InternalNode partiallyResolvedLayout) {
    return new LayoutResultHolder(partiallyResolvedLayout);
  }
}
