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

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.animation.FiniteAnimationSpec
import com.facebook.litho.animation.transition
import com.facebook.litho.animation.translationX
import com.facebook.litho.animation.translationY
import com.facebook.litho.animation.tween
import com.facebook.litho.animation.useAnimationGroup
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import kotlin.time.Duration

/**
 * Choreographs parallel, sequenced, and staggered motion with [Style.transition].
 *
 * One tap moves every block to the other end of its track. Each block animates its own layout
 * bounds, and the `delay` on its [tween] sets when it starts:
 * - **Parallel**: every block uses the same spec.
 * - **Sequence**: each block starts when the previous one finishes.
 * - **Stagger**: starts are offset by less than the duration, so the motions overlap. The last
 *   block waits an extra fixed delay.
 * - **Same component**: one block moves down, then across. [Style.transition] animates `bounds`
 *   with a single spec, so this row drives `translationY` and a delayed `translationX` from one
 *   [useAnimationGroup] instead.
 *
 * Every row reacts to the same state change, and each block's transition runs independently.
 */
@OptIn(ExperimentalLithoApi::class)
class ChoreographyComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val atEnd = useState { false }

    // start_example
    val parallel = List(BLOCK_COLORS.size) { tween<Float>(DURATION) }
    val sequence = List(BLOCK_COLORS.size) { i -> tween<Float>(DURATION, delay = DURATION * i) }
    val stagger =
        List(BLOCK_COLORS.size) { i ->
          val extraDelay = if (i == BLOCK_COLORS.lastIndex) EXTRA_DELAY else Duration.ZERO
          tween<Float>(DURATION, delay = STAGGER_STEP * i + extraDelay)
        }
    // end_example

    val travelX = (TRACK_WIDTH.toPixels() - BLOCK_WIDTH.toPixels()).toFloat()
    val travelY = (SAME_TRACK_HEIGHT.toPixels() - BLOCK_HEIGHT.toPixels()).toFloat()
    val sameBlockMotion = useAnimationGroup(atEnd.value, label = "sameComponent")
    val sameBlockY =
        sameBlockMotion.animateFloat(
            transitionSpec = { tween(DURATION) },
            label = "y",
        ) { end ->
          if (end) travelY else 0f
        }
    val sameBlockX =
        sameBlockMotion.animateFloat(
            transitionSpec = { tween(DURATION, delay = DURATION) },
            label = "x",
        ) { end ->
          if (end) travelX else 0f
        }

    return Column(style = Style.padding(all = 16.dp).onClick { atEnd.update { !it } }) {
      child(
          Text(
              text = "Tap anywhere to toggle",
              textSize = 16.sp,
              textColor = Color.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      child(rowLabel("Parallel"))
      child(blockTrack(atEnd.value, parallel))
      child(rowLabel("Sequence"))
      child(blockTrack(atEnd.value, sequence))
      child(rowLabel("Stagger, last block delayed"))
      child(blockTrack(atEnd.value, stagger))
      child(rowLabel("Same component: Y, then X"))
      child(
          Column(
              style =
                  Style.width(TRACK_WIDTH).height(SAME_TRACK_HEIGHT).backgroundColor(TRACK_COLOR),
          ) {
            child(
                Column(
                    style =
                        Style.width(BLOCK_WIDTH)
                            .height(BLOCK_HEIGHT)
                            .backgroundColor(BLOCK_COLORS.first())
                            .translationX(sameBlockX)
                            .translationY(sameBlockY),
                ),
            )
          },
      )
    }
  }

  private fun ResourcesScope.rowLabel(text: String): Component = Text(
      text = text,
      textSize = 14.sp,
      textColor = Color.DKGRAY,
      style = Style.margin(bottom = 4.dp),
  )

  private fun ResourcesScope.blockTrack(
      atEnd: Boolean,
      specs: List<FiniteAnimationSpec<Float>>,
  ): Component = Column(
      alignItems = if (atEnd) YogaAlign.FLEX_END else YogaAlign.FLEX_START,
      style = Style.width(TRACK_WIDTH).margin(bottom = 16.dp).backgroundColor(TRACK_COLOR),
  ) {
    specs.forEachIndexed { index, spec ->
      child(
          Column(
              style =
                  Style.width(BLOCK_WIDTH)
                      .height(BLOCK_HEIGHT)
                      .margin(vertical = 2.dp)
                      .backgroundColor(BLOCK_COLORS[index])
                      .transition { bounds = spec },
          ),
      )
    }
  }
}

private val DURATION = 400.ms
private val STAGGER_STEP = 120.ms
private val EXTRA_DELAY = 300.ms

private val TRACK_WIDTH = 320.dp
private val SAME_TRACK_HEIGHT = 72.dp
private val BLOCK_WIDTH = 48.dp
private val BLOCK_HEIGHT = 24.dp

private val TRACK_COLOR = "#EEF1F5".toColorInt()
private val BLOCK_COLORS = listOf(
    "#4285F4".toColorInt(),
    "#34A853".toColorInt(),
    "#9C27B0".toColorInt(),
)
