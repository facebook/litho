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
import com.facebook.litho.animation.transition
import com.facebook.litho.animation.tween
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.key
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.view.Shape
import com.facebook.litho.view.alpha
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.clip
import com.facebook.litho.view.clipChildren
import com.facebook.litho.view.onClick
import com.facebook.litho.view.scale
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Page indicators with a window of dots that slides as the selection nears an edge.
 *
 * Ten pages share a window of five dots. Every dot stays mounted, keyed by its page. When the
 * window shifts, each dot's layout position changes and [Style.transition] animates its bounds. The
 * selected dot and its neighbours are full size and the window's edge dots are smaller. Dots
 * outside the window sit at scale 0 and alpha 0, so they shrink and fade out as they leave and grow
 * back when they enter.
 */
@OptIn(ExperimentalLithoApi::class)
class PageIndicatorsComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val window = useState { PageWindow(selected = 0, firstVisible = 0) }
    val current = window.value

    return Column(alignItems = YogaAlign.CENTER, style = Style.padding(all = 24.dp)) {
      child(
          Text(
              text = "${current.selected + 1}/$PAGE_COUNT",
              textSize = 20.sp,
              textColor = "#333333".toColorInt(),
          ),
      )
      child(
          Row(
              style =
                  Style.width((SLOT_DP * WINDOW_SIZE).dp)
                      .margin(vertical = 24.dp)
                      .clipChildren(true),
          ) {
            child(
                Row(
                    alignItems = YogaAlign.CENTER,
                    style =
                        Style.width((SLOT_DP * PAGE_COUNT).dp)
                            .flex(shrink = 0f)
                            .margin(start = (-SLOT_DP * current.firstVisible).dp),
                ) {
                  for (page in 0 until PAGE_COUNT) {
                    child(indicatorDot(page, current))
                  }
                },
            )
          },
      )
      child(
          Row {
            child(pagerButton("Prev", window, step = -1))
            child(pagerButton("Next", window, step = 1))
          },
      )
    }
  }

  // start_example
  @SuppressLint("AlphaStyleDetector") // Off-window dots fade out; they have no click handling.
  private fun ResourcesScope.indicatorDot(page: Int, window: PageWindow): Component =
      key("dot$page") {
        val position = page - window.firstVisible
        val inWindow = position in 0 until WINDOW_SIZE
        Row(
            alignItems = YogaAlign.CENTER,
            justifyContent = YogaJustify.CENTER,
            style = Style.width(SLOT_DP.dp).height(SLOT_DP.dp),
        ) {
          child(
              Column(
                  style =
                      Style.width(DOT_DP.dp)
                          .height(DOT_DP.dp)
                          .backgroundColor(if (page == window.selected) Color.BLACK else Color.GRAY)
                          .clip(Shape.Circle)
                          .scale(if (inWindow) dotScale(position, window) else 0f)
                          .alpha(if (inWindow) 1f else 0f)
                          .transition {
                            bounds = tween(MOVE_DURATION)
                            scale = tween(MOVE_DURATION)
                            alpha = tween(MOVE_DURATION)
                          },
              ),
          )
        }
      }

  // end_example

  private fun ResourcesScope.pagerButton(
      label: String,
      window: State<PageWindow>,
      step: Int,
  ): Component = Text(
      text = label,
      textSize = 16.sp,
      textColor = Color.WHITE,
      style =
          Style.backgroundColor("#4285F4".toColorInt())
              .padding(horizontal = 20.dp, vertical = 10.dp)
              .margin(horizontal = 8.dp)
              .onClick { window.update { it.step(step) } },
  )
}

/**
 * Scale for the dot at [position] in the window. When the window is pinned to the first or last
 * page and the selection is near that end, the far edge tapers; otherwise both edge dots are
 * medium.
 */
private fun dotScale(position: Int, window: PageWindow): Float =
    when {
      window.firstVisible == 0 && window.selected <= MAX_LARGE_DOT_COUNT - 1 ->
          when (position) {
            WINDOW_SIZE - 1 -> SCALE_SMALL
            WINDOW_SIZE - 2 -> SCALE_MEDIUM
            else -> SCALE_LARGE
          }
      window.firstVisible == PAGE_COUNT - WINDOW_SIZE &&
          window.selected >= PAGE_COUNT - MAX_LARGE_DOT_COUNT ->
          when (position) {
            0 -> SCALE_SMALL
            1 -> SCALE_MEDIUM
            else -> SCALE_LARGE
          }
      position == 0 || position == WINDOW_SIZE - 1 -> SCALE_MEDIUM
      else -> SCALE_LARGE
    }

private class PageWindow(val selected: Int, val firstVisible: Int) {
  /** Moves the selection by [delta], shifting the window when it reaches the window's edge. */
  fun step(delta: Int): PageWindow {
    val next = (selected + delta).coerceIn(0, PAGE_COUNT - 1)
    if (next == selected) {
      return this
    }
    val lastVisible = firstVisible + WINDOW_SIZE - 1
    val first =
        when {
          delta > 0 && lastVisible < PAGE_COUNT - 1 && next >= lastVisible -> firstVisible + 1
          delta < 0 && firstVisible > 0 && next <= firstVisible -> firstVisible - 1
          else -> firstVisible
        }
    return PageWindow(next, first)
  }
}

private const val PAGE_COUNT = 10
private const val WINDOW_SIZE = 5
private const val MAX_LARGE_DOT_COUNT = 3
private const val SLOT_DP = 24
private const val DOT_DP = 14
private const val SCALE_LARGE = 1f
private const val SCALE_MEDIUM = 0.75f
private const val SCALE_SMALL = 0.5f
private val MOVE_DURATION = 300.ms
