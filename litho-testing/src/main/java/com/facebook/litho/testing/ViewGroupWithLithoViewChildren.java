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

package com.facebook.litho.testing;

import android.content.Context;
import android.view.ViewGroup;
import com.facebook.litho.HasLithoViewChildren;
import com.facebook.litho.LithoView;
import java.util.List;

public class ViewGroupWithLithoViewChildren extends ViewGroup implements HasLithoViewChildren {

  public ViewGroupWithLithoViewChildren(Context context) {
    super(context);
  }

  @Override
  public void obtainLithoViewChildren(List<LithoView> lithoViews) {
    for (int i = 0, size = getChildCount(); i < size; i++) {
      lithoViews.add((LithoView) getChildAt(i));
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {}
}
