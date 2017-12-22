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

  /**
   * Show the given tooltip with the specified offsets from the bottom-left corner of the component
   * with the given anchorKey. If there are multiple components with this key, the tooltip will be
   * shown on the first one that is found in a breath-first order.
   */
  public static void showTooltip(
      ComponentContext c,
      final PopupWindow popupWindow,
      String anchorKey,
      int xOffset,
      int yOffset) {
    showTooltip(
        c,
        new LithoTooltip() {
          @Override
          public void showLithoTooltip(
              View container, Rect anchorBounds, int xOffset, int yOffset) {
            popupWindow.showAsDropDown(
                container, anchorBounds.left + xOffset, anchorBounds.bottom + yOffset);
          }
        },
        anchorKey,
        xOffset,
        yOffset);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey. If there are multiple
   * components with this key, the tooltip will be shown on the first one that is found in a
   * breath-first order.
   *
   * @param c
   * @param lithoTooltip A {@link LithoTooltip} implementation to be shown on the anchor.
   * @param anchorKey key of the Litho Component that will be used as anchor
   */
  public static void showTooltip(ComponentContext c, LithoTooltip lithoTooltip, String anchorKey) {
    showTooltip(c, lithoTooltip, anchorKey, 0, 0);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey.
   *
   * @param c
   * @param lithoTooltip A {@link LithoTooltip} implementation to be shown on the anchor.
   * @param anchorKey key of the Litho Component that will be used as anchor
   * @param xOffset horizontal offset from default position where the tooltip shows.
   * @param yOffset vertical offset from default position where the tooltip shows.
   */
  public static void showTooltip(
      ComponentContext c, LithoTooltip lithoTooltip, String anchorKey, int xOffset, int yOffset) {
    final ComponentTree componentTree = c.getComponentTree();

    if (componentTree == null) {
      return;
    }

    componentTree.showTooltip(c, lithoTooltip, anchorKey, xOffset, yOffset);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey in the specified position. If
   * there are multiple components with this key, the tooltip will be shown on the first one that is
   * found in a breath-first order.
   *
   * @deprecated @see {#show}
   */
  @Deprecated
  public static void showTooltip(
      ComponentContext c,
      DeprecatedLithoTooltip tooltip,
      String anchorKey,
      TooltipPosition tooltipPosition) {
    showTooltip(c, tooltip, anchorKey, tooltipPosition, 0, 0);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey in the specified position. If
   * there are multiple components with this key, the tooltip will be shown on the first one that is
   * found in a breath-first order.
   *
   * @deprecated @see {@link #showTooltip(ComponentContext, PopupWindow, String, int, int)}
   */
  @Deprecated
  public static void showTooltip(
      ComponentContext c,
      final PopupWindow popupWindow,
      String anchorKey,
      TooltipPosition tooltipPosition) {
    showTooltip(c, popupWindow, anchorKey, tooltipPosition, 0, 0);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey with the specified offsets
   * from the given position. If there are multiple components with this key, the tooltip will be
   * shown on the first one that is found in a breath-first order.
   *
   * @deprecated @see {@link #showTooltip(ComponentContext, PopupWindow, String, int, int)}
   */
  @Deprecated
  public static void showTooltip(
      ComponentContext c,
      final PopupWindow popupWindow,
      String anchorKey,
      TooltipPosition tooltipPosition,
      int xOffset,
      int yOffset) {
    showTooltip(
        c,
        new DeprecatedLithoTooltip() {
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
   * from the given position. If there are multiple components with this key, the tooltip will be
   * shown on the first one that is found in a breath-first order.
   *
   * @deprecated
   * @see {{@link #showTooltip(ComponentContext, LithoTooltip, String, int, int)}}
   */
  @Deprecated
  public static void showTooltip(
      ComponentContext c,
      DeprecatedLithoTooltip tooltip,
      String anchorKey,
      TooltipPosition tooltipPosition,
      int xOffset,
      int yOffset) {
    final ComponentTree componentTree = c.getComponentTree();

    if (componentTree == null) {
      return;
    }

    componentTree.showTooltip(c, tooltip, anchorKey, tooltipPosition, xOffset, yOffset);
  }

  @Deprecated
  static void showOnAnchor(
      DeprecatedLithoTooltip tooltip,
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
