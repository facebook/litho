/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.graphics.Rect;
import android.view.View;
import android.widget.PopupWindow;

public class LithoTooltipController {
  /** Show the given tooltip on the component with the given anchorKey in the specified position. */
  public static void showTooltip(
      ComponentContext c, LithoTooltip tooltip, String anchorKey, TooltipPosition tooltipPosition) {
    showTooltip(c, tooltip, anchorKey, tooltipPosition, 0, 0);
  }

  /** Show the given tooltip on the component with the given anchorKey in the specified position. */
  public static void showTooltip(
      ComponentContext c,
      final PopupWindow popupWindow,
      String anchorKey,
      TooltipPosition tooltipPosition) {
    showTooltip(c, popupWindow, anchorKey, tooltipPosition, 0, 0);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey with the specified offsets
   * from the given position.
   */
  public static void showTooltip(
      ComponentContext c,
      final PopupWindow popupWindow,
      String anchorKey,
      TooltipPosition tooltipPosition,
      int xOffset,
      int yOffset) {
    showTooltip(
        c,
        new LithoTooltip() {
          @Override
          public void showBottomLeft(View anchor, int xOffset, int yOffset) {
            popupWindow.showAsDropDown(anchor, xOffset, yOffset);
          }
        },
        anchorKey,
        tooltipPosition,
        xOffset,
        yOffset);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey with the specified offsets
   * from the given position.
   */
  public static void showTooltip(
      ComponentContext c,
      LithoTooltip tooltip,
      String anchorKey,
      TooltipPosition tooltipPosition,
      int xOffset,
      int yOffset) {
    final ComponentTree componentTree = c.getComponentTree();
    final Component rootComponent = c.getComponentScope();

    if (componentTree == null) {
      return;
    }

    final String anchorGlobalKey = rootComponent == null
        ? anchorKey
        : rootComponent.getGlobalKey() + anchorKey;

    componentTree.showTooltip(tooltip, anchorGlobalKey, tooltipPosition, xOffset, yOffset);
  }

  static void showOnAnchor(
      LithoTooltip tooltip,
      Rect anchorBounds,
      View hostView,
      TooltipPosition tooltipPosition,
      int xOffset,
      int yOffset) {
    final int topOffset = anchorBounds.top - hostView.getHeight();
    final int bottomOffset = anchorBounds.bottom - hostView.getHeight();
    final int centerXOffset = anchorBounds.left + (anchorBounds.right - anchorBounds.left) / 2;
    final int centerYOffset =
        (anchorBounds.top + (anchorBounds.bottom - anchorBounds.top) / 2) - hostView.getHeight();

    final int xoff, yoff;

    switch (tooltipPosition) {
      case CENTER:
        xoff = centerXOffset;
        yoff = centerYOffset;
        break;
      case CENTER_LEFT:
        xoff = anchorBounds.left;
        yoff = centerYOffset;
        break;
      case TOP_LEFT:
        xoff = anchorBounds.left;
        yoff = topOffset;
        break;
      case CENTER_TOP:
        xoff = centerXOffset;
        yoff = topOffset;
        break;
      case TOP_RIGHT:
        xoff = anchorBounds.right;
        yoff = topOffset;
        break;
      case CENTER_RIGHT:
        xoff = anchorBounds.right;
        yoff = centerYOffset;
        break;
      case BOTTOM_RIGHT:
        xoff = anchorBounds.right;
        yoff = bottomOffset;
        break;
      case CENTER_BOTTOM:
        xoff = centerXOffset;
        yoff = bottomOffset;
        break;
      case BOTTOM_LEFT:
      default:
        xoff = anchorBounds.left;
        yoff = bottomOffset;
    }

    tooltip.showBottomLeft(hostView, xoff + xOffset, yoff + yOffset);
  }
}
