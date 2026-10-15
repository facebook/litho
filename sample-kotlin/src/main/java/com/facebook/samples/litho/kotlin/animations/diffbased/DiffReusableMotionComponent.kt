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

/** Uses one reusable transition style across a small card deck. */
@OptIn(ExperimentalLithoApi::class)
class DiffReusableMotionComponent : KComponent() {
  @SuppressLint("AlphaStyleDetector") // Alpha is the animated property being demonstrated.
  override fun ComponentScope.render(): Component {
    val selected = useState { "inbox" }
    val cardMotion = Style.transition {
      bounds = tween(duration = 220.ms, easing = Easing.FastOutSlowIn)
      alpha = tween(duration = 140.ms)
      scale = spring()
    }

    return Column(style = Style.padding(all = 24.dp)) {
      child(
          Text(
              text = "Tap a card",
              textSize = 16.sp,
              textColor = Color.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      for (card in CARDS) {
        child(deckCard(card, selected, cardMotion))
      }
    }
  }

  private fun ResourcesScope.deckCard(
      card: DemoCard,
      selected: State<String>,
      motion: Style,
  ): Component {
    val isSelected = selected.value == card.id
    return Row(
        alignItems = YogaAlign.CENTER,
        justifyContent = YogaJustify.SPACE_BETWEEN,
        style =
            (Style.width(if (isSelected) 320.dp else 292.dp)
                    .height(if (isSelected) 76.dp else 62.dp)
                    .margin(bottom = 12.dp)
                    .backgroundColor(card.color)
                    .padding(horizontal = 18.dp)
                    .alpha(if (isSelected) 1f else 0.72f)
                    .scale(if (isSelected) 1f else 0.96f) + motion)
                .onClick { selected.update(card.id) },
    ) {
      child(
          Text(
              text = card.title,
              textSize = if (isSelected) 20.sp else 16.sp,
              textColor = Color.WHITE,
          ),
      )
      if (isSelected) {
        child(
            Text(
                text = "selected",
                textSize = 15.sp,
                textColor = Color.WHITE,
                style =
                    Style.backgroundColor("#22000000".toColorInt())
                        .padding(horizontal = 10.dp, vertical = 6.dp) + motion,
            ),
        )
      }
    }
  }
}

private class DemoCard(
    val id: String,
    val title: String,
    val color: Int,
)

private val CARDS = listOf(
    DemoCard("inbox", "Inbox", "#4285F4".toColorInt()),
    DemoCard("planned", "Planned", "#34A853".toColorInt()),
    DemoCard("review", "Review", "#FBBC04".toColorInt()),
    DemoCard("blocked", "Blocked", "#9C27B0".toColorInt()),
    DemoCard("done", "Done", "#EA4335".toColorInt()),
)
