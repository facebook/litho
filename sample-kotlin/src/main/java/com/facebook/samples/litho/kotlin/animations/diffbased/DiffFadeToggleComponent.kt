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

package com.facebook.samples.litho.kotlin.animations.diffbased

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.animation.Easing
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.transition
import com.facebook.litho.animation.tween
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.view.scale
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/** Fade and scale between two layout states without explicit from/to values. */
@OptIn(ExperimentalLithoApi::class)
class DiffFadeToggleComponent : KComponent() {
  @SuppressLint("AlphaStyleDetector") // Alpha is the animated property being demonstrated.
  override fun ComponentScope.render(): Component {
    val visible = useState { true }

    return Column(style = Style.padding(all = 24.dp)) {
      child(
          Text(
              text = "Tap to toggle",
              textSize = 16.sp,
              textColor = Color.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      child(
          Column(
              style =
                  Style.width(320.dp)
                      .height(250.dp)
                      .backgroundColor("#4285F4".toColorInt())
                      .padding(all = 20.dp)
                      .alpha(if (visible.value) 1f else 0.32f)
                      .scale(if (visible.value) 1f else 0.94f)
                      .transition {
                        alpha = tween(duration = 180.ms, easing = Easing.FastOutSlowIn)
                        scale = spring()
                      }
                      .onClick { visible.update { !it } },
          ) {
            child(
                Text(
                    text = if (visible.value) "Visible" else "Dimmed",
                    textSize = 26.sp,
                    textColor = Color.WHITE,
                ),
            )
            child(
                Text(
                    text =
                        if (visible.value) {
                          "alpha = 1.0, scale = 1.0"
                        } else {
                          "alpha = 0.32, scale = 0.94"
                        },
                    textSize = 17.sp,
                    textColor = Color.WHITE,
                ),
            )
            child(metricRow("State", if (visible.value) "Active" else "Muted"))
            child(metricRow("Opacity", if (visible.value) "100%" else "32%"))
            child(metricRow("Scale", if (visible.value) "1.00" else "0.94"))
          },
      )
    }
  }

  private fun ResourcesScope.metricRow(label: String, value: String): Component = Row(
      alignItems = YogaAlign.CENTER,
      justifyContent = YogaJustify.SPACE_BETWEEN,
      style =
          Style.width(280.dp)
              .height(34.dp)
              .margin(top = 12.dp)
              .backgroundColor("#22000000".toColorInt())
              .padding(horizontal = 12.dp),
  ) {
    child(Text(text = label, textSize = 14.sp, textColor = Color.WHITE))
    child(Text(text = value, textSize = 14.sp, textColor = Color.WHITE))
  }
}
