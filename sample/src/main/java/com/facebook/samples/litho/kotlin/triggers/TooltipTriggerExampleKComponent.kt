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

package com.facebook.samples.litho.kotlin.triggers

import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.ColorInt
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.Handle
import com.facebook.litho.KComponent
import com.facebook.litho.LithoTooltipController
import com.facebook.litho.LithoView
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.handle
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCached
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.visibility.onVisible
import com.facebook.yoga.YogaAlign

class TooltipTriggerExampleKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    // creating_handle_start
    val anchorHandle = useCached { Handle() }
    // creating_handle_end

    return Column(alignItems = YogaAlign.CENTER) {
      child(
          Text(
              text = "Click to trigger/show tooltip",
              defStyleAttr = android.R.attr.buttonStyle,
              typeface = Typeface.DEFAULT_BOLD,
              style =
                  Style.padding(horizontal = 20.dp, vertical = 25.dp).onClick {
                    showToolTip(anchorHandle)
                  }))
      child(
          // using_handle_start
          handle(anchorHandle) {
            Text(
                text = "Tooltip anchor",
                style = Style.margin(top = 50.dp).onVisible { showToolTip(anchorHandle) })
          }
          // using_handle_end
          )
    }
  }

  // showing_tooltip_start
  private fun ComponentScope.showToolTip(anchorHandle: Handle) {
    LithoTooltipController.showTooltipOnHandle(
        context, createTooltip("Example Tooltip"), anchorHandle, 0, 0)
  }
  // showing_tooltip_end

  private fun ComponentScope.createTooltip(tooltipText: String) =
      PopupWindow(
          LithoView.create(
              context,
              Column(style = Style.padding(all = 15.dp).backgroundColor(LITHO_PINK)) {
                child(Text(text = tooltipText, textColor = Color.WHITE))
              }),
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
          true)

  private companion object {
    @ColorInt private val LITHO_PINK = -0xc9481
  }
}
