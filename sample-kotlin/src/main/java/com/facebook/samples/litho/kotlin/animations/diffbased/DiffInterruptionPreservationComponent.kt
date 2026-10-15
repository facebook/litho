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
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.animation.DampingRatio
import com.facebook.litho.animation.Easing
import com.facebook.litho.animation.Stiffness
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.transition
import com.facebook.litho.animation.tween
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.key
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Shows that an in-flight diff-based animation survives being interrupted, in two ways:
 * 1. **Retarget mid-flight** — flipping the target of a running spring reverses it from its current
 *    position with its current velocity, instead of snapping to the start or stalling for a frame.
 * 2. **Keyed remount** — swapping the animating component for a different one under the same key
 *    keeps the running animation alive instead of restarting it.
 */
@OptIn(ExperimentalLithoApi::class)
class DiffInterruptionPreservationComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val atEnd = useState { false }
    val dimmed = useState { false }
    val detailed = useState { false }

    return Column(style = Style.padding(all = 24.dp)) {
      child(heading("Interruption Preservation"))

      child(
          caption(
              "Retarget mid-flight",
              "Tap the track to send the puck across, then tap again before it lands. It reverses from wherever it is, keeping its momentum, instead of snapping back or freezing for a frame.",
          ),
      )
      child(interruptibleTrack(atEnd))

      child(
          caption(
              "Preserved across a keyed remount",
              "Start the fade, then swap the card variant before it finishes. The stable key keeps the running alpha animation going instead of restarting it.",
          ),
      )
      child(remountControls(detailed, dimmed))
      child(remountStage(detailed, dimmed))
    }
  }

  private fun ResourcesScope.heading(text: String): Component = Text(
      text = text,
      textSize = 22.sp,
      textColor = "#1F1F1F".toColorInt(),
      style = Style.margin(bottom = 20.dp),
  )

  private fun ResourcesScope.caption(title: String, body: String): Component =
      Column(style = Style.width(360.dp).margin(bottom = 12.dp)) {
        child(
            Text(
                text = title,
                textSize = 16.sp,
                textColor = "#1F1F1F".toColorInt(),
                style = Style.margin(bottom = 4.dp),
            ),
        )
        child(Text(text = body, textSize = 14.sp, textColor = Color.DKGRAY))
      }

  private fun ResourcesScope.interruptibleTrack(atEnd: State<Boolean>): Component = Row(
      alignItems = YogaAlign.CENTER,
      justifyContent = if (atEnd.value) YogaJustify.FLEX_END else YogaJustify.FLEX_START,
      style =
          Style.width(320.dp)
              .height(104.dp)
              .margin(bottom = 28.dp)
              .padding(all = 12.dp)
              .backgroundColor("#EEF1F5".toColorInt())
              .onClick { atEnd.update { !it } },
  ) {
    child(
        Row(
            alignItems = YogaAlign.CENTER,
            justifyContent = YogaJustify.CENTER,
            style =
                Style.width(80.dp)
                    .height(80.dp)
                    .backgroundColor("#3F51B5".toColorInt())
                    .transition {
                      bounds =
                          spring(
                              stiffness = Stiffness.Low,
                              dampingRatio = DampingRatio.MediumBouncy,
                          )
                    },
        ) {
          child(Text(text = "spring", textSize = 13.sp, textColor = Color.WHITE))
        },
    )
  }

  private fun ResourcesScope.remountControls(
      detailed: State<Boolean>,
      dimmed: State<Boolean>,
  ): Component =
      Row(alignItems = YogaAlign.CENTER, style = Style.width(360.dp).margin(bottom = 12.dp)) {
        child(
            chip(
                label = if (dimmed.value) "Reset fade" else "Start fade",
                backgroundColor = "#E8F5E9".toColorInt(),
                style = Style.margin(end = 12.dp),
            ) {
              dimmed.update { !it }
            },
        )
        child(
            chip(
                label = if (detailed.value) "Show summary" else "Show detailed",
                backgroundColor = "#E3F2FD".toColorInt(),
            ) {
              detailed.update { !it }
            },
        )
      }

  private fun ResourcesScope.chip(
      label: String,
      backgroundColor: Int,
      style: Style = Style,
      onClick: () -> Unit,
  ): Component = Row(
      alignItems = YogaAlign.CENTER,
      justifyContent = YogaJustify.CENTER,
      style =
          (Style.height(40.dp).padding(horizontal = 16.dp).backgroundColor(backgroundColor) + style)
              .onClick { onClick() },
  ) {
    child(Text(text = label, textSize = 14.sp, textColor = "#16324F".toColorInt()))
  }

  private fun ResourcesScope.remountStage(
      detailed: State<Boolean>,
      dimmed: State<Boolean>,
  ): Component = Column(
      style = Style.width(360.dp).padding(all = 16.dp).backgroundColor("#F6F8FA".toColorInt()),
  ) {
    child(
        Text(
            text = "Keyed slot",
            textSize = 13.sp,
            textColor = Color.GRAY,
            style = Style.margin(bottom = 12.dp),
        ),
    )
    child(
        key("preserved_card") {
          if (detailed.value) {
            DetailedRemountCard(dimmed = dimmed.value)
          } else {
            SummaryRemountCard(dimmed = dimmed.value)
          }
        },
    )
  }
}

@OptIn(ExperimentalLithoApi::class)
private class SummaryRemountCard(private val dimmed: Boolean) : KComponent() {
  override fun ComponentScope.render(): Component =
      Column(style = remountCardStyle(dimmed = dimmed, detailed = false)) {
        child(
            Text(
                text = "Summary",
                textSize = 18.sp,
                textColor = Color.WHITE,
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(
            Text(
                text = "Compact variant in the preserved slot.",
                textSize = 13.sp,
                textColor = "#DDE4FF".toColorInt(),
            ),
        )
      }
}

@OptIn(ExperimentalLithoApi::class)
private class DetailedRemountCard(private val dimmed: Boolean) : KComponent() {
  override fun ComponentScope.render(): Component =
      Column(style = remountCardStyle(dimmed = dimmed, detailed = true)) {
        child(
            Text(
                text = "Detailed",
                textSize = 18.sp,
                textColor = Color.WHITE,
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(
            Text(
                text = "A different component type mounted under the same key.",
                textSize = 13.sp,
                textColor = "#DDE4FF".toColorInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(
            Text(
                text = "Bounds change too, so the host resizes in the same pass.",
                textSize = 12.sp,
                textColor = "#B9C6FF".toColorInt(),
            ),
        )
      }
}

@OptIn(ExperimentalLithoApi::class)
@SuppressLint("AlphaStyleDetector") // Alpha is one of the animated properties being demonstrated.
private fun remountCardStyle(dimmed: Boolean, detailed: Boolean): Style =
    Style.width(if (detailed) 320.dp else 292.dp)
        .height(if (detailed) 168.dp else 120.dp)
        .padding(all = 16.dp)
        .backgroundColor("#3F51B5".toColorInt())
        .alpha(if (dimmed) 0.22f else 1f)
        .transition {
          bounds = spring(stiffness = Stiffness.Medium, dampingRatio = DampingRatio.MediumBouncy)
          alpha = tween(duration = 1500.ms, easing = Easing.FastOutSlowIn)
        }
