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
import com.facebook.litho.ComponentsReporter.emitMessage
import com.facebook.litho.ThreadUtils.assertMainThread

object LithoTooltipController {
  private const val INVALID_HANDLE = "LithoTooltipController:InvalidHandle"

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
      yOffset: Int = 0,
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
      yOffset: Int,
  ) {
    assertMainThread()
    val anchorBounds = layoutState.findBoundsForHandle(handle)
    if (anchorBounds == null) {
      val name = componentContext.componentScope?.simpleName ?: "null"
      emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          INVALID_HANDLE,
          """Cannot find a component with handle $handle to use as anchor.
Component: $name""",
      )
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
      yOffset: Int,
  ) {
    showTooltipOnHandle(
        c,
        { container, anchorBounds, x, y ->
          popupWindow.showAsDropDown(container, anchorBounds.left + x, anchorBounds.bottom + y)
        },
        handle,
        xOffset,
        yOffset,
    )
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
      showTooltip: (container: View, anchorBounds: Rect) -> Unit,
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
        yOffset = 0,
    )
  }
}
