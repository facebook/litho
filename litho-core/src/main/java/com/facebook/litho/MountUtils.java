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

import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

public class MountUtils {

  /**
   * Traverses this view's LithoView children and if incremental mount is enabled it forces a mount
   * on the child LithoView even if it's outside the visible bounds. This will not also trigger
   * visibility events since they will not be accurate.
   */
  static void ensureAllLithoViewChildrenAreMounted(View view) {
    assertMainThread();

    if (view instanceof LithoView) {
      final LithoView lithoView = (LithoView) view;
      if (lithoView.isIncrementalMountEnabled()) {
        lithoView.notifyVisibleBoundsChanged(
            new Rect(0, 0, view.getWidth(), view.getHeight()), false);
      }
    } else if (view instanceof ViewGroup) {
      final ViewGroup viewGroup = (ViewGroup) view;

      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        final View childView = viewGroup.getChildAt(i);
        ensureAllLithoViewChildrenAreMounted(childView);
      }
    }
  }
}
