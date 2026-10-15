/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore.simplelist;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.LayoutContext;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderResult;
import com.facebook.rendercore.RenderResultFuture;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import java.util.List;

/**
 * Util class for scrollable containers to measure the size they should be using based on their own
 * specifications and children's sizes.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class MeasureListUtils {

  private static final int UNSET = -1;
  private static final int WIDTH = 0;
  private static final int HEIGHT = 1;
  public static final Rect NO_PADDING = new Rect();

  private MeasureListUtils() {}

  public static int getDefaultChildWidthSpec(int orientation, int measuredParentWidth) {
    return orientation == RecyclerView.VERTICAL
        ? measuredParentWidth == 0
            ? MeasureSpecUtils.unspecified()
            : MeasureSpecUtils.exactly(measuredParentWidth)
        : MeasureSpecUtils.unspecified();
  }

  public static int getDefaultChildHeightSpec(int orientation, int measuredParentHeight) {
    return orientation == RecyclerView.VERTICAL
        ? MeasureSpecUtils.unspecified()
        : measuredParentHeight == 0
            ? MeasureSpecUtils.unspecified()
            : MeasureSpecUtils.exactly(measuredParentHeight);
  }

  /**
   * Measures the total size of a list of items, accounting for spacing and per-child margins.
   *
   * @param childMargins Optional per-child margins. Each Rect contains (left, top, right, bottom)
   *     margins for the corresponding child. Margins are added to each child's effective size when
   *     computing the collection's total dimensions.
   */
  public static <RenderContext, T extends Node<RenderContext>> int[] measureListSize(
      LayoutContext<RenderContext> c,
      int parentWidthSpec,
      int parentHeightSpec,
      int spaceBefore,
      int spaceBetween,
      int spaceAfter,
      int orientation,
      boolean reverse,
      List<ListItem<T>> listItems,
      @Nullable List<Rect> childMargins,
      Rect padding) {
    final boolean isHorizontalScroll = orientation == RecyclerView.HORIZONTAL;
    final boolean isVerticalScroll = orientation == RecyclerView.VERTICAL;
    final int widthMode = View.MeasureSpec.getMode(parentWidthSpec);
    final int heightMode = View.MeasureSpec.getMode(parentHeightSpec);

    final int horizontalPadding = padding.left + padding.right;
    final int verticalPadding = padding.top + padding.bottom;
    final int mainAxisLeadingPadding = isVerticalScroll ? padding.top : padding.left;
    final int mainAxisTrailingPadding = isVerticalScroll ? padding.bottom : padding.right;

    final int[] size = new int[] {UNSET, UNSET};

    final int maxWidthAllowed = View.MeasureSpec.getSize(parentWidthSpec);
    if (widthMode == EXACTLY) {
      size[WIDTH] = maxWidthAllowed;
    }

    final int maxHeightAllowed = View.MeasureSpec.getSize(parentHeightSpec);
    if (heightMode == EXACTLY) {
      size[HEIGHT] = maxHeightAllowed;
    }

    if (widthMode == EXACTLY && heightMode == EXACTLY) {
      return size;
    }

    final int horizontalSpaceBefore = isHorizontalScroll ? spaceBefore + mainAxisLeadingPadding : 0;
    final int horizontalSpaceBetween = isHorizontalScroll ? spaceBetween : 0;
    final int horizontalSpaceAfter = isHorizontalScroll ? spaceAfter + mainAxisTrailingPadding : 0;
    final int verticalSpaceBefore = isVerticalScroll ? spaceBefore + mainAxisLeadingPadding : 0;
    final int verticalSpaceBetween = isVerticalScroll ? spaceBetween : 0;
    final int verticalSpaceAfter = isVerticalScroll ? spaceAfter + mainAxisTrailingPadding : 0;

    int combinedWidth = 0;
    int combinedHeight = 0;
    int currentMaxWidth = 0;
    int currentMaxHeight = 0;

    int index = reverse ? listItems.size() - 1 : 0;
    while (reverse ? index >= 0 : index < listItems.size()) {
      final RenderResultFuture<?, ?> renderResultFuture =
          listItems.get(index).getRenderResultFuture();
      final RenderResult<?, ?> renderResult = renderResultFuture.runAndGet();
      final RenderTree renderTree = renderResult.getRenderTree();
      final boolean isFirstItem = index == 0;
      final boolean isNotLastItem = index != listItems.size() - 1;

      // Account for per-child margins in effective size
      final int horizontalMargins;
      final int verticalMargins;
      if (childMargins != null && index < childMargins.size()) {
        final Rect m = childMargins.get(index);
        horizontalMargins = m.left + m.right;
        verticalMargins = m.top + m.bottom;
      } else {
        horizontalMargins = 0;
        verticalMargins = 0;
      }

      final int childWidth = renderTree.getWidth() + horizontalMargins;
      final int offsetChildWidth;
      if (isNotLastItem) {
        offsetChildWidth =
            isFirstItem
                ? (horizontalSpaceBefore + childWidth + horizontalSpaceBetween)
                : (childWidth + horizontalSpaceBetween);
      } else {
        offsetChildWidth =
            isFirstItem
                ? (horizontalSpaceBefore + childWidth + horizontalSpaceAfter)
                : (childWidth + horizontalSpaceAfter);
      }
      currentMaxWidth = Math.max(currentMaxWidth, offsetChildWidth);
      combinedWidth += offsetChildWidth;

      if (widthMode == AT_MOST) {
        if ((isHorizontalScroll && combinedWidth >= maxWidthAllowed)
            || (isVerticalScroll && currentMaxWidth + horizontalPadding >= maxWidthAllowed)) {
          size[WIDTH] = maxWidthAllowed;
        }
      }

      final int childHeight = renderTree.getHeight() + verticalMargins;
      final int offsetChildHeight;
      if (isNotLastItem) {
        offsetChildHeight =
            isFirstItem
                ? (verticalSpaceBefore + childHeight + verticalSpaceBetween)
                : (childHeight + verticalSpaceBetween);
      } else {
        offsetChildHeight =
            isFirstItem
                ? (verticalSpaceBefore + childHeight + verticalSpaceAfter)
                : (childHeight + verticalSpaceAfter);
      }

      currentMaxHeight = Math.max(currentMaxHeight, offsetChildHeight);
      combinedHeight += offsetChildHeight;

      if (heightMode == AT_MOST) {
        if ((isVerticalScroll && combinedHeight >= maxHeightAllowed)
            || (isHorizontalScroll && currentMaxHeight + verticalPadding >= maxHeightAllowed)) {
          size[HEIGHT] = maxHeightAllowed;
        }
      }

      if (isWidthMeasured(size) && isHeightMeasured(size)) {
        break;
      }

      if (reverse) {
        index--;
      } else {
        index++;
      }
    }

    if (widthMode == UNSPECIFIED) {
      size[WIDTH] = isHorizontalScroll ? combinedWidth : currentMaxWidth + horizontalPadding;
    } else if (widthMode == AT_MOST) {
      // We will only reach here if all the children are measured and the total width hasn't
      // exceeded the max width allowed.
      size[WIDTH] =
          isHorizontalScroll
              ? Math.min(combinedWidth, maxWidthAllowed)
              : Math.min(currentMaxWidth + horizontalPadding, maxWidthAllowed);
    }

    if (heightMode == UNSPECIFIED) {
      size[HEIGHT] = isVerticalScroll ? combinedHeight : currentMaxHeight + verticalPadding;
    } else if (heightMode == AT_MOST) {
      size[HEIGHT] =
          isVerticalScroll
              ? Math.min(combinedHeight, maxHeightAllowed)
              : Math.min(currentMaxHeight + verticalPadding, maxHeightAllowed);
    }

    return size;
  }

  /**
   * V2 measurement that supports wrap-content and per-child margins.
   *
   * @param childMargins Optional per-child margins. Each Rect contains (left, top, right, bottom)
   *     margins for the corresponding child. Margins are added to each child's effective size when
   *     computing the collection's total dimensions.
   */
  public static <RenderContext, T extends Node<RenderContext>> int[] measureListSizeV2(
      LayoutContext<RenderContext> c,
      int parentWidthSpec,
      int parentHeightSpec,
      int spaceBefore,
      int spaceBetween,
      int spaceAfter,
      int orientation,
      boolean wrapWidth,
      boolean wrapHeight,
      boolean reverse,
      List<ListItem<T>> listItems,
      @Nullable List<Rect> childMargins,
      Rect padding) {
    final boolean isHorizontalScroll = orientation == RecyclerView.HORIZONTAL;
    final boolean isVerticalScroll = orientation == RecyclerView.VERTICAL;
    final int widthMode = View.MeasureSpec.getMode(parentWidthSpec);
    final int heightMode = View.MeasureSpec.getMode(parentHeightSpec);

    final int horizontalPadding = padding.left + padding.right;
    final int verticalPadding = padding.top + padding.bottom;
    final int mainAxisLeadingPadding = isVerticalScroll ? padding.top : padding.left;
    final int mainAxisTrailingPadding = isVerticalScroll ? padding.bottom : padding.right;

    final int[] size = new int[] {UNSET, UNSET};

    final int maxWidthAllowed =
        widthMode != UNSPECIFIED ? View.MeasureSpec.getSize(parentWidthSpec) : Integer.MAX_VALUE;
    if (!wrapWidth) {
      size[WIDTH] = maxWidthAllowed;
    }

    final int maxHeightAllowed =
        heightMode != UNSPECIFIED ? View.MeasureSpec.getSize(parentHeightSpec) : Integer.MAX_VALUE;
    if (!wrapHeight) {
      size[HEIGHT] = maxHeightAllowed;
    }

    if (isWidthMeasured(size) && isHeightMeasured(size)) {
      return size;
    }

    final int horizontalSpaceBefore = isHorizontalScroll ? spaceBefore + mainAxisLeadingPadding : 0;
    final int horizontalSpaceBetween = isHorizontalScroll ? spaceBetween : 0;
    final int horizontalSpaceAfter = isHorizontalScroll ? spaceAfter + mainAxisTrailingPadding : 0;
    final int verticalSpaceBefore = isVerticalScroll ? spaceBefore + mainAxisLeadingPadding : 0;
    final int verticalSpaceBetween = isVerticalScroll ? spaceBetween : 0;
    final int verticalSpaceAfter = isVerticalScroll ? spaceAfter + mainAxisTrailingPadding : 0;

    int combinedWidth = 0;
    int combinedHeight = 0;
    int currentMaxWidth = 0;
    int currentMaxHeight = 0;

    int index = reverse ? listItems.size() - 1 : 0;
    while (reverse ? index >= 0 : index < listItems.size()) {
      final RenderResultFuture<?, ?> renderResultFuture =
          listItems.get(index).getRenderResultFuture();
      final RenderResult<?, ?> renderResult = renderResultFuture.runAndGet();
      final RenderTree renderTree = renderResult.getRenderTree();
      final boolean isFirstItem = index == 0;
      final boolean isNotLastItem = index != listItems.size() - 1;

      // Account for per-child margins in effective size
      final int horizontalMargins;
      final int verticalMargins;
      if (childMargins != null && index < childMargins.size()) {
        final Rect m = childMargins.get(index);
        horizontalMargins = m.left + m.right;
        verticalMargins = m.top + m.bottom;
      } else {
        horizontalMargins = 0;
        verticalMargins = 0;
      }

      final int childWidth = renderTree.getWidth() + horizontalMargins;
      final int offsetChildWidth;
      if (isNotLastItem) {
        offsetChildWidth =
            isFirstItem
                ? (horizontalSpaceBefore + childWidth + horizontalSpaceBetween)
                : (childWidth + horizontalSpaceBetween);
      } else {
        offsetChildWidth =
            isFirstItem
                ? (horizontalSpaceBefore + childWidth + horizontalSpaceAfter)
                : (childWidth + horizontalSpaceAfter);
      }
      currentMaxWidth = Math.max(currentMaxWidth, offsetChildWidth);
      combinedWidth += offsetChildWidth;

      if (!isWidthMeasured(size)) {
        if ((isHorizontalScroll && combinedWidth >= maxWidthAllowed)
            || (isVerticalScroll && currentMaxWidth + horizontalPadding >= maxWidthAllowed)) {
          size[WIDTH] = maxWidthAllowed;
        }
      }

      final int childHeight = renderTree.getHeight() + verticalMargins;
      final int offsetChildHeight;
      if (isNotLastItem) {
        offsetChildHeight =
            isFirstItem
                ? (verticalSpaceBefore + childHeight + verticalSpaceBetween)
                : (childHeight + verticalSpaceBetween);
      } else {
        offsetChildHeight =
            isFirstItem
                ? (verticalSpaceBefore + childHeight + verticalSpaceAfter)
                : (childHeight + verticalSpaceAfter);
      }

      currentMaxHeight = Math.max(currentMaxHeight, offsetChildHeight);
      combinedHeight += offsetChildHeight;

      if (heightMode != UNSPECIFIED) {
        if ((isVerticalScroll && combinedHeight >= maxHeightAllowed)
            || (isHorizontalScroll && currentMaxHeight + verticalPadding >= maxHeightAllowed)) {
          size[HEIGHT] = maxHeightAllowed;
        }
      }

      if (isWidthMeasured(size) && isHeightMeasured(size)) {
        break;
      }

      if (reverse) {
        index--;
      } else {
        index++;
      }
    }

    if (!isWidthMeasured(size)) {
      if (widthMode == UNSPECIFIED) {
        size[WIDTH] = isHorizontalScroll ? combinedWidth : currentMaxWidth + horizontalPadding;
      } else {
        // We will only reach here if all the children are measured and the total width hasn't
        // exceeded the max width allowed.
        size[WIDTH] =
            isHorizontalScroll
                ? Math.min(combinedWidth, maxWidthAllowed)
                : Math.min(currentMaxWidth + horizontalPadding, maxWidthAllowed);
      }
    }

    if (!isHeightMeasured(size)) {
      if (heightMode == UNSPECIFIED) {
        size[HEIGHT] = isVerticalScroll ? combinedHeight : currentMaxHeight + verticalPadding;
      } else {
        size[HEIGHT] =
            isVerticalScroll
                ? Math.min(combinedHeight, maxHeightAllowed)
                : Math.min(currentMaxHeight + verticalPadding, maxHeightAllowed);
      }
    }

    return size;
  }

  private static boolean isWidthMeasured(int[] size) {
    return size[WIDTH] > UNSET;
  }

  private static boolean isHeightMeasured(int[] size) {
    return size[HEIGHT] > UNSET;
  }
}
