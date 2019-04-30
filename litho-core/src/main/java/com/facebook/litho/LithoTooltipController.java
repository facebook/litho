/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import android.graphics.Rect;
import android.view.View;
import android.widget.PopupWindow;
import javax.annotation.Nullable;

public class LithoTooltipController {

  /**
   * Show the given tooltip with the specified offsets from the bottom-left corner of the root
   * component.
   */
  public static void showTooltipOnRootComponent(
      ComponentContext c, final PopupWindow popupWindow, int xOffset, int yOffset) {
    showTooltip(c, popupWindow, null, xOffset, yOffset);
  }

  /**
   * Show the given tooltip on the root component.
   *
   * @param c
   * @param lithoTooltip A {@link LithoTooltip} implementation to be shown on the root component.
   */
  public static void showTooltipOnRootComponent(ComponentContext c, LithoTooltip lithoTooltip) {
    showTooltip(c, lithoTooltip, null);
  }

  /**
   * Show the given tooltip on the root component.
   *
   * @param c
   * @param lithoTooltip A {@link LithoTooltip} implementation to be shown on the root component.
   * @param xOffset horizontal offset from default position where the tooltip shows.
   * @param yOffset vertical offset from default position where the tooltip shows.
   */
  public static void showTooltipOnRootComponent(
      ComponentContext c, LithoTooltip lithoTooltip, int xOffset, int yOffset) {
    showTooltip(c, lithoTooltip, null, xOffset, yOffset);
  }

  /**
   * Show the given tooltip with the specified offsets from the bottom-left corner of the component
   * with the given anchorKey.
   */
  public static void showTooltip(
      ComponentContext c,
      final PopupWindow popupWindow,
      @Nullable String anchorKey,
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
   * Show the given tooltip on the component with the given anchorKey.
   *
   * @param c
   * @param lithoTooltip A {@link LithoTooltip} implementation to be shown on the anchor.
   * @param anchorKey key of the Litho Component that will be used as anchor. If unset, the root
   *     component will be used as the anchor.
   */
  public static void showTooltip(
      ComponentContext c, LithoTooltip lithoTooltip, @Nullable String anchorKey) {
    showTooltip(c, lithoTooltip, anchorKey, 0, 0);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey.
   *
   * @param c
   * @param lithoTooltip A {@link LithoTooltip} implementation to be shown on the anchor.
   * @param anchorKey key of the Litho Component that will be used as anchor. If unset, the root
   *     component will be used as the anchor.
   * @param xOffset horizontal offset from default position where the tooltip shows.
   * @param yOffset vertical offset from default position where the tooltip shows.
   */
  public static void showTooltip(
      ComponentContext c,
      LithoTooltip lithoTooltip,
      @Nullable String anchorKey,
      int xOffset,
      int yOffset) {
    final ComponentTree componentTree = c.getComponentTree();
    final Component rootComponent = c.getComponentScope();

    if (componentTree == null || componentTree.isReleased() || !componentTree.hasMounted()) {
      return;
    }

    final String anchorGlobalKey;
    if (rootComponent == null && anchorKey == null) {
      return;
    } else if (rootComponent == null) {
      anchorGlobalKey = anchorKey;
    } else if (anchorKey == null) {
      anchorGlobalKey = rootComponent.getGlobalKey();
    } else {
      anchorGlobalKey =
          ComponentKeyUtils.getKeyWithSeparator(rootComponent.getGlobalKey(), anchorKey);
    }

    componentTree.showTooltip(lithoTooltip, anchorGlobalKey, xOffset, yOffset);
  }

  /**
   * Show the given tooltip on the component with the given anchorKey in the specified position.
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
   * Show the given tooltip on the component with the given anchorKey in the specified position.
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
   * from the given position.
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
   * from the given position.
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
    final Component rootComponent = c.getComponentScope();

    if (componentTree == null) {
      return;
    }

    final String anchorGlobalKey =
        rootComponent == null
            ? anchorKey
            : ComponentKeyUtils.getKeyWithSeparator(rootComponent.getGlobalKey(), anchorKey);

    componentTree.showTooltip(tooltip, anchorGlobalKey, tooltipPosition, xOffset, yOffset);
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
