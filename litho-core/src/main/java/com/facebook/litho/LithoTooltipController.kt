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

package com.facebook.litho

import android.graphics.Rect
import android.view.View
import android.widget.PopupWindow
import com.facebook.litho.ComponentKeyUtils.getKeyWithSeparator
import com.facebook.litho.ComponentsReporter.emitMessage
import com.facebook.litho.ThreadUtils.assertMainThread

object LithoTooltipController {
  private const val INVALID_KEY = "LithoTooltipController:InvalidKey"
  private const val INVALID_HANDLE = "LithoTooltipController:InvalidHandle"

  /**
   * Show the given tooltip with the specified offsets from the bottom-left corner of the root
   * component.
   */
  @JvmStatic
  fun showTooltipOnRootComponent(
      c: ComponentContext,
      popupWindow: PopupWindow,
      xOffset: Int,
      yOffset: Int
  ) {
    showTooltip(c, popupWindow, null, xOffset, yOffset)
  }

  /**
   * Show the given tooltip on the root component.
   *
   * @param c
   * @param lithoTooltip A [LithoTooltip] implementation to be shown on the root component.
   */
  @JvmStatic
  fun showTooltipOnRootComponent(c: ComponentContext, lithoTooltip: LithoTooltip) {
    showTooltip(c, lithoTooltip, null)
  }

  /**
   * Show the given tooltip on the root component.
   *
   * @param c
   * @param lithoTooltip A [LithoTooltip] implementation to be shown on the root component.
   * @param xOffset horizontal offset from default position where the tooltip shows.
   * @param yOffset vertical offset from default position where the tooltip shows.
   */
  @JvmStatic
  fun showTooltipOnRootComponent(
      c: ComponentContext,
      lithoTooltip: LithoTooltip,
      xOffset: Int,
      yOffset: Int
  ) {
    showTooltip(c, lithoTooltip, null, xOffset, yOffset)
  }

  /**
   * Show the given tooltip on the component with the given handle instance.
   *
   * @param c
   * @param lithoTooltip A [LithoTooltip] implementation to be shown on the anchor.
   * @param handle A [Handle] used to discover the object in the hierarchy.
   * @param xOffset horizontal offset from default position where the tooltip shows.
   * @param yOffset vertical offset from default position where the tooltip shows.
   */
  @JvmStatic
  @JvmOverloads
  fun showTooltipOnHandle(
      c: ComponentContext,
      lithoTooltip: LithoTooltip,
      handle: Handle,
      xOffset: Int = 0,
      yOffset: Int = 0
  ) {
    handle.mountedViewReference?.let { reference ->
      val mountedView: View? = reference.mountedView
      if (mountedView == null || mountedView !is LithoView) {
        return
      }
      mountedView.mountedLayoutState?.let { layoutState ->
        showTooltipOnHandle(layoutState, mountedView, c, lithoTooltip, handle, xOffset, yOffset)
      }
    }
  }

  @JvmStatic
  fun showTooltipOnHandle(
      layoutState: LayoutState,
      lithoView: LithoView?,
      componentContext: ComponentContext,
      lithoTooltip: LithoTooltip,
      handle: Handle,
      xOffset: Int,
      yOffset: Int
  ) {
    assertMainThread()
    val componentHandleToBounds: Map<Handle, Rect> = layoutState.componentHandleToBounds
    val anchorBounds = componentHandleToBounds[handle]
    if (anchorBounds == null) {
      val name = componentContext.componentScope?.simpleName ?: "null"
      emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          INVALID_HANDLE,
          """Cannot find a component with handle $handle to use as anchor.
Component: $name""")
      return
    }
    lithoTooltip.showLithoTooltip(lithoView, anchorBounds, xOffset, yOffset)
  }

  /**
   * Show the given tooltip on the component with the given handle instance.
   *
   * @param c
   * @param popupWindow A [PopupWindow] implementation to be shown in the tooltip.
   * @param handle A [Handle] used to discover the object in the hierarchy.
   * @param xOffset horizontal offset from default position where the tooltip shows.
   * @param yOffset vertical offset from default position where the tooltip shows.
   */
  @JvmStatic
  fun showTooltipOnHandle(
      c: ComponentContext,
      popupWindow: PopupWindow,
      handle: Handle,
      xOffset: Int,
      yOffset: Int
  ) {
    showTooltipOnHandle(
        c,
        { container, anchorBounds, x, y ->
          popupWindow.showAsDropDown(container, anchorBounds.left + x, anchorBounds.bottom + y)
        },
        handle,
        xOffset,
        yOffset)
  }

  /**
   * Will invoke [showTooltip] for the given [handle] if it has a valid container. The code of
   * [showTooltip] should be the one responsible for drawing the tooltip in the screen and is left
   * for the client implementation.
   */
  @JvmStatic
  fun showTooltipOnHandle(
      c: ComponentContext,
      handle: Handle,
      showTooltip: (container: View, anchorBounds: Rect) -> Unit
  ) {
    showTooltipOnHandle(
        c = c,
        lithoTooltip = { container, anchorBounds, _, _ ->
          if (container != null) {
            showTooltip(container, anchorBounds)
          }
        },
        handle = handle,
        xOffset = 0,
        yOffset = 0)
  }

  /**
   * Show the given tooltip with the specified offsets from the bottom-left corner of the component
   * with the given anchorKey.
   */
  @Deprecated("@see [showTooltipOnHandle(ComponentContext, PopupWindow, Handle, int, int)]")
  @JvmStatic
  fun showTooltip(
      context: ComponentContext,
      popupWindow: PopupWindow,
      anchorKey: String?,
      xOffset: Int,
      yOffset: Int
  ) {
    showTooltip(
        context,
        { container, anchorBounds, x, y ->
          popupWindow.showAsDropDown(container, anchorBounds.left + x, anchorBounds.bottom + y)
        },
        anchorKey,
        xOffset,
        yOffset)
  }

  /** Show the given tooltip on the component with the given anchorKey. */
  @Deprecated("@see [showTooltipOnHandle(ComponentContext, LithoTooltip, Handle)]")
  @JvmStatic
  fun showTooltip(c: ComponentContext, lithoTooltip: LithoTooltip, anchorKey: String?) {
    showTooltip(c, lithoTooltip, anchorKey, 0, 0)
  }

  /** Show the given tooltip on the component with the given anchorKey. */
  @Deprecated("@see [showTooltipOnHandle(ComponentContext, LithoTooltip, Handle, int, int)]")
  @JvmStatic
  fun showTooltip(
      c: ComponentContext,
      lithoTooltip: LithoTooltip,
      anchorKey: String?,
      xOffset: Int,
      yOffset: Int
  ) {

    val mountedView: View? = c.mountedView
    if (mountedView == null || mountedView !is LithoView) {
      return
    }
    mountedView.mountedLayoutState?.let { layoutState ->
      val rootComponent: Component? = c.componentScope
      val anchorGlobalKey: String? =
          if (rootComponent == null && anchorKey == null) {
            return
          } else if (rootComponent == null) {
            anchorKey
          } else if (anchorKey == null) {
            c.globalKey
          } else {
            getKeyWithSeparator(c.globalKey, anchorKey)
          }
      showTooltip(
          layoutState, mountedView, lithoTooltip, requireNotNull(anchorGlobalKey), xOffset, yOffset)
    }
  }

  private fun showTooltip(
      layoutState: LayoutState,
      lithoView: LithoView,
      lithoTooltip: LithoTooltip,
      anchorGlobalKey: String,
      xOffset: Int,
      yOffset: Int
  ) {
    assertMainThread()
    val componentKeysToBounds: Map<String, Rect> = layoutState.componentKeyToBounds
    val anchorBounds: Rect? = componentKeysToBounds[anchorGlobalKey]
    if (!componentKeysToBounds.containsKey(anchorGlobalKey) || anchorBounds == null) {
      emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          INVALID_KEY,
          "Cannot find a component with key $anchorGlobalKey to use as anchor.")
      return
    }
    lithoTooltip.showLithoTooltip(lithoView, anchorBounds, xOffset, yOffset)
  }

  @Deprecated("")
  private fun showTooltip(
      layoutState: LayoutState,
      lithoView: LithoView,
      tooltip: DeprecatedLithoTooltip,
      anchorGlobalKey: String,
      tooltipPosition: TooltipPosition,
      xOffset: Int,
      yOffset: Int
  ) {
    assertMainThread()
    val componentKeysToBounds: Map<String, Rect> = layoutState.componentKeyToBounds
    val anchorBounds: Rect? = componentKeysToBounds[anchorGlobalKey]
    if (!componentKeysToBounds.containsKey(anchorGlobalKey) || anchorBounds == null) {
      emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          INVALID_KEY,
          "Cannot find a component with key $anchorGlobalKey to use as anchor.")
      return
    }
    showOnAnchor(tooltip, anchorBounds, lithoView, tooltipPosition, xOffset, yOffset)
  }

  /** Show the given tooltip on the component with the given anchorKey in the specified position. */
  @JvmStatic
  fun showTooltip(
      c: ComponentContext,
      tooltip: DeprecatedLithoTooltip,
      anchorKey: String,
      tooltipPosition: TooltipPosition
  ) {
    showTooltip(c, tooltip, anchorKey, tooltipPosition, 0, 0)
  }

  /**
   * Show the given tooltip on the component with the given anchorKey with the specified offsets
   * from the given position.
   */
  @Deprecated("@see [showTooltip(ComponentContext, PopupWindow, String, int, int)]")
  @JvmStatic
  fun showTooltip(
      c: ComponentContext,
      popupWindow: PopupWindow,
      anchorKey: String,
      tooltipPosition: TooltipPosition,
      xOffset: Int,
      yOffset: Int
  ) {
    showTooltip(
        c,
        { anchor, x, y -> popupWindow.showAsDropDown(anchor, x, y) },
        anchorKey,
        tooltipPosition,
        xOffset,
        yOffset)
  }

  /**
   * Show the given tooltip on the component with the given anchorKey with the specified offsets
   * from the given position.
   */
  @Deprecated("@see [showTooltip(ComponentContext, LithoTooltip, String, int, int)]")
  @JvmStatic
  fun showTooltip(
      context: ComponentContext,
      tooltip: DeprecatedLithoTooltip,
      anchorKey: String,
      tooltipPosition: TooltipPosition,
      xOffset: Int,
      yOffset: Int,
  ) {
    val rootComponent = context.componentScope
    val mountedView = context.mountedView
    if (mountedView == null || mountedView !is LithoView) {
      return
    }
    mountedView.mountedLayoutState?.let { layoutState ->
      val anchorGlobalKey =
          if (rootComponent == null) {
            anchorKey
          } else {
            getKeyWithSeparator(context.globalKey, anchorKey)
          }
      showTooltip(
          layoutState, mountedView, tooltip, anchorGlobalKey, tooltipPosition, xOffset, yOffset)
    }
  }

  @Deprecated("")
  @JvmStatic
  fun showOnAnchor(
      tooltip: DeprecatedLithoTooltip,
      anchorBounds: Rect,
      hostView: View,
      tooltipPosition: TooltipPosition,
      xOffset: Int,
      yOffset: Int
  ) {
    val topOffset = anchorBounds.top - hostView.height
    val bottomOffset = anchorBounds.bottom - hostView.height
    val centerXOffset = anchorBounds.left + (anchorBounds.right - anchorBounds.left) / 2
    val centerYOffset =
        anchorBounds.top + (anchorBounds.bottom - anchorBounds.top) / 2 - hostView.height
    val xoff: Int
    val yoff: Int
    when (tooltipPosition) {
      TooltipPosition.CENTER -> {
        xoff = centerXOffset
        yoff = centerYOffset
      }
      TooltipPosition.CENTER_LEFT -> {
        xoff = anchorBounds.left
        yoff = centerYOffset
      }
      TooltipPosition.TOP_LEFT -> {
        xoff = anchorBounds.left
        yoff = topOffset
      }
      TooltipPosition.CENTER_TOP -> {
        xoff = centerXOffset
        yoff = topOffset
      }
      TooltipPosition.TOP_RIGHT -> {
        xoff = anchorBounds.right
        yoff = topOffset
      }
      TooltipPosition.CENTER_RIGHT -> {
        xoff = anchorBounds.right
        yoff = centerYOffset
      }
      TooltipPosition.BOTTOM_RIGHT -> {
        xoff = anchorBounds.right
        yoff = bottomOffset
      }
      TooltipPosition.CENTER_BOTTOM -> {
        xoff = centerXOffset
        yoff = bottomOffset
      }
      TooltipPosition.BOTTOM_LEFT -> {
        xoff = anchorBounds.left
        yoff = bottomOffset
      }
      else -> {
        xoff = anchorBounds.left
        yoff = bottomOffset
      }
    }
    tooltip.showBottomLeft(hostView, xoff + xOffset, yoff + yOffset)
  }
}
