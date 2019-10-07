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

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** Utility class for calculating the wrapped height of given holders. */
public class LayoutInfoUtils {

  private LayoutInfoUtils() {}

  public static int getTopDecorationHeight(RecyclerView.LayoutManager layoutManager, int position) {
    final View itemView = layoutManager.getChildAt(position);
    return (itemView != null) ? layoutManager.getTopDecorationHeight(itemView) : 0;
  }

  public static int getBottomDecorationHeight(
      RecyclerView.LayoutManager layoutManager, int position) {
    final View itemView = layoutManager.getChildAt(position);
    return (itemView != null) ? layoutManager.getBottomDecorationHeight(itemView) : 0;
  }

  /**
   * Return the accumulated height of ComponentTreeHolders, or return the {@param maxHeight} if the
   * accumulated height is higher than the {@param maxHeight}.
   */
  public static int computeLinearLayoutWrappedHeight(
      LinearLayoutManager linearLayoutManager,
      int maxHeight,
      List<ComponentTreeHolder> componentTreeHolders) {
    final int itemCount = componentTreeHolders.size();
    int measuredHeight = 0;

    switch (linearLayoutManager.getOrientation()) {
      case LinearLayoutManager.VERTICAL:
        for (int i = 0; i < itemCount; i++) {
          final ComponentTreeHolder holder = componentTreeHolders.get(i);

          measuredHeight += holder.getMeasuredHeight();
          measuredHeight += LayoutInfoUtils.getTopDecorationHeight(linearLayoutManager, i);
          measuredHeight += LayoutInfoUtils.getBottomDecorationHeight(linearLayoutManager, i);

          if (measuredHeight > maxHeight) {
            measuredHeight = maxHeight;
            break;
          }
        }
        return measuredHeight;

      case LinearLayoutManager.HORIZONTAL:
      default:
        throw new IllegalStateException(
            "This method should only be called when orientation is vertical");
    }
  }

  /**
   * Return the max height in the {@param componentTreeHolders}, the range is from position {@param
   * start} to position {@param end} (excluded).
   */
  public static int getMaxHeightInRow(
      int start, int end, List<ComponentTreeHolder> componentTreeHolders) {
    final int itemCount = componentTreeHolders.size();

    int measuredHeight = 0;
    for (int i = start; i < end && i < itemCount; i++) {
      final ComponentTreeHolder holder = componentTreeHolders.get(i);
      measuredHeight = Math.max(measuredHeight, holder.getMeasuredHeight());
    }
    return measuredHeight;
  }
}
