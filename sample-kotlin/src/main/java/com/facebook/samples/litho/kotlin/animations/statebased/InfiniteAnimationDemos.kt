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

package com.facebook.samples.litho.kotlin.animations.statebased

import android.graphics.Color
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.animation.Easing
import com.facebook.litho.animation.RepeatMode
import com.facebook.litho.animation.StartOffset
import com.facebook.litho.animation.StartOffsetType
import com.facebook.litho.animation.alpha
import com.facebook.litho.animation.backgroundColor
import com.facebook.litho.animation.rotation
import com.facebook.litho.animation.scale
import com.facebook.litho.animation.translationX
import com.facebook.litho.animation.translationY
import com.facebook.litho.animation.tween
import com.facebook.litho.animation.useInfiniteAnimationGroup
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.size
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.StackLayout
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.FlexboxScope
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useCallback
import com.facebook.litho.useState
import com.facebook.litho.view.Shape
import com.facebook.litho.view.background
import com.facebook.litho.view.clip
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.graphics.Color as LithoColor
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

@OptIn(ExperimentalLithoApi::class)
class InfiniteAnimationDemosComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val running = useState { true }
    val toggleRunning = useCallback { -> running.update { !it } }

    return InfiniteDemoSurface {
      child(
          Row(
              alignItems = YogaAlign.CENTER,
              justifyContent = YogaJustify.SPACE_BETWEEN,
              style = Style.widthPercent(100f).margin(bottom = 16.dp),
          ) {
            child(
                Column(style = Style.flex(grow = 1f)) {
                  child(Text("Infinite animations", textSize = 22.sp, textColor = INK))
                  child(
                      Text(
                          "Lifecycle-owned loops for pulse, spinner, shimmer, and staged motion",
                          textSize = 13.sp,
                          textColor = MUTED,
                          style = Style.margin(top = 4.dp),
                      ),
                  )
                },
            )
            child(RunToggle(running.value, toggleRunning))
          },
      )
      child(PulseDemoComponent(running.value))
      child(
          Row(style = Style.margin(top = 12.dp), alignItems = YogaAlign.STRETCH) {
            child(SpinnerDemoComponent(running.value, Style.flex(grow = 1f).margin(end = 12.dp)))
            child(TypingDotsDemoComponent(running.value, Style.flex(grow = 1f)))
          },
      )
      child(ShimmerDemoComponent(running.value, Style.margin(top = 12.dp)))
    }
  }
}

@OptIn(ExperimentalLithoApi::class)
private class PulseDemoComponent(private val running: Boolean) : KComponent() {

  override fun ComponentScope.render(): Component {
    val infinite = useInfiniteAnimationGroup(shouldRun = running, label = "sample/pulse")
    val scale =
        infinite.animateFloat(
            from = 0.92f,
            to = 1.08f,
            animation = tween(680.ms),
            label = "scale",
        )
    val alpha =
        infinite.animateFloat(
            from = 0.56f,
            to = 1f,
            animation = tween(680.ms),
            label = "alpha",
        )
    val color =
        infinite.animateColor(
            from = LithoColor.fromInt(0xFF2563EB.toInt()),
            to = LithoColor.fromInt(0xFF14B8A6.toInt()),
            animation = tween(1_360.ms),
            label = "color",
        )

    return DemoCard {
      child(DemoLabel("Pulse"))
      child(
          Row(
              alignItems = YogaAlign.CENTER,
              style = Style.widthPercent(100f).margin(top = 14.dp),
          ) {
            child(
                Column(
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.CENTER,
                    style =
                        Style.size(72.dp)
                            .scale(scale)
                            .alpha(alpha)
                            .clip(Shape.Circle)
                            .backgroundColor(color),
                ) {
                  child(
                      Column(
                          style =
                              Style.size(24.dp).background(RoundedRect(0xFFFFFFFF.toInt(), 12.dp)),
                      ),
                  )
                },
            )
            child(
                Column(style = Style.flex(grow = 1f).margin(start = 18.dp)) {
                  child(Text("Attention cue", textSize = 16.sp, textColor = INK))
                  child(
                      Text(
                          "Reverse mode is the ergonomic default for breathing UI loops.",
                          textSize = 13.sp,
                          textColor = MUTED,
                          style = Style.margin(top = 4.dp),
                      ),
                  )
                },
            )
          },
      )
    }
  }
}

@OptIn(ExperimentalLithoApi::class)
private class SpinnerDemoComponent(
    private val running: Boolean,
    private val style: Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val infinite = useInfiniteAnimationGroup(shouldRun = running, label = "sample/spinner")
    val rotation =
        infinite.animateFloat(
            from = 0f,
            to = 360f,
            animation = tween(1_000.ms, easing = Easing.Linear),
            repeatMode = RepeatMode.Restart,
            label = "rotation",
        )

    return DemoCard(style) {
      child(DemoLabel("Spinner"))
      child(
          Column(
              alignItems = YogaAlign.CENTER,
              justifyContent = YogaJustify.CENTER,
              style = Style.widthPercent(100f).height(112.dp).margin(top = 12.dp),
          ) {
            child(
                StackLayout(style = Style.size(58.dp).rotation(rotation)) {
                  child(
                      Column(style = Style.size(58.dp).background(RoundedRect(0xFFE0F2FE, 29.dp))),
                  )
                  child(
                      Column(
                          style =
                              Style.width(18.dp)
                                  .height(18.dp)
                                  .margin(start = 20.dp)
                                  .background(RoundedRect(0xFF0284C7.toInt(), 9.dp)),
                      ),
                  )
                },
            )
          },
      )
    }
  }
}

@OptIn(ExperimentalLithoApi::class)
private class TypingDotsDemoComponent(
    private val running: Boolean,
    private val style: Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val infinite = useInfiniteAnimationGroup(shouldRun = running, label = "sample/dots")
    val offsets =
        (0..2).map { index ->
          infinite.animateFloat(
              from = 0f,
              to = -16f,
              animation = tween(360.ms),
              initialStartOffset = StartOffset(index * 120, StartOffsetType.FastForward),
              label = "dot$index",
          )
        }

    return DemoCard(style) {
      child(DemoLabel("Typing dots"))
      child(
          Row(
              alignItems = YogaAlign.CENTER,
              justifyContent = YogaJustify.CENTER,
              style = Style.widthPercent(100f).height(112.dp).margin(top = 12.dp),
          ) {
            offsets.forEachIndexed { index, offset ->
              child(
                  Column(
                      style =
                          Style.size(16.dp)
                              .margin(horizontal = 4.dp)
                              .translationY(offset)
                              .background(RoundedRect(DOT_COLORS[index], 8.dp)),
                  ),
              )
            }
          },
      )
    }
  }
}

@OptIn(ExperimentalLithoApi::class)
private class ShimmerDemoComponent(
    private val running: Boolean,
    private val style: Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val infinite = useInfiniteAnimationGroup(shouldRun = running, label = "sample/shimmer")
    val translateX =
        infinite.animateFloat(
            from = -220f,
            to = 260f,
            animation = tween(1_300.ms, easing = Easing.Linear),
            repeatMode = RepeatMode.Restart,
            label = "sweep",
        )
    val shine =
        infinite.animateFloat(
            from = 0.28f,
            to = 0.52f,
            animation = tween(1_300.ms),
            label = "shine",
        )

    return DemoCard(style) {
      child(DemoLabel("Shimmer"))
      child(
          StackLayout(
              style =
                  Style.widthPercent(100f)
                      .height(118.dp)
                      .margin(top = 14.dp)
                      .clip(Shape.RoundedRectangle(18.dp))
                      .background(RoundedRect(0xFFF1F5F9, 18.dp)),
          ) {
            child(
                Column(style = Style.widthPercent(100f).padding(all = 16.dp)) {
                  child(SkeletonLine(0xFFE2E8F0.toInt(), Style.width(170.dp)))
                  child(
                      SkeletonLine(0xFFE5E7EB.toInt(), Style.widthPercent(88f).margin(top = 12.dp)),
                  )
                  child(
                      SkeletonLine(0xFFE5E7EB.toInt(), Style.widthPercent(64f).margin(top = 10.dp)),
                  )
                },
            )
            child(
                Column(
                    style =
                        Style.width(74.dp)
                            .height(118.dp)
                            .translationX(translateX)
                            .alpha(shine)
                            .background(RoundedRect(0xFFFFFFFF.toInt(), 18.dp)),
                ),
            )
          },
      )
    }
  }
}

private fun ResourcesScope.InfiniteDemoSurface(content: FlexboxScope.() -> Unit): Component =
    Column(style = Style.padding(all = 18.dp).background(RoundedRect(Color.WHITE, 0.dp))) {
      content()
    }

private fun ResourcesScope.DemoCard(
    style: Style = Style,
    content: FlexboxScope.() -> Unit,
): Component = Column(
    style =
        style
            .widthPercent(100f)
            .padding(all = 16.dp)
            .background(RoundedRect(0xFFFFFFFF.toInt(), 18.dp)),
) {
  content()
}

private fun ResourcesScope.DemoLabel(text: String): Component = Text(
    text = text,
    textSize = 13.sp,
    textColor = MUTED,
    style =
        Style.padding(horizontal = 10.dp, vertical = 5.dp)
            .background(RoundedRect(0xFFEFF6FF, 14.dp)),
)

private fun ResourcesScope.RunToggle(
    running: Boolean,
    onClick: () -> Unit,
): Component = Column(
    alignItems = YogaAlign.CENTER,
    justifyContent = YogaJustify.CENTER,
    style =
        Style.height(38.dp)
            .padding(horizontal = 14.dp)
            .background(RoundedRect(if (running) 0xFF0F766E else 0xFFE2E8F0, 19.dp))
            .onClick { onClick() },
) {
  child(
      Text(
          if (running) "Running" else "Paused",
          textSize = 13.sp,
          textColor = if (running) Color.WHITE else INK,
      ),
  )
}

private fun ResourcesScope.SkeletonLine(
    color: Int,
    style: Style,
): Component = Column(style = style.height(14.dp).background(RoundedRect(color, 7.dp)))

private val DOT_COLORS = intArrayOf(0xFF2563EB.toInt(), 0xFF0F766E.toInt(), 0xFFDB2777.toInt())

private val INK = 0xFF0F172A.toInt()
private val MUTED = 0xFF64748B.toInt()
