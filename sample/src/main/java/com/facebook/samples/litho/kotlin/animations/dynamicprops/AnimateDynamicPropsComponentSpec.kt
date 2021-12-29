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

package com.facebook.samples.litho.kotlin.animations.dynamicprops

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.DynamicValue
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.State
import com.facebook.litho.eventHandler
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

// start_example
@LayoutSpec
object AnimateDynamicPropsComponentSpec {

  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      time: StateValue<DynamicValue<Long>>,
      animator: StateValue<AtomicReference<ValueAnimator?>>,
  ) {
    time.set(DynamicValue<Long>(0))
    animator.set(AtomicReference<ValueAnimator?>(null))
  }

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State time: DynamicValue<Long>,
      @State animator: AtomicReference<ValueAnimator?>,
  ): Component? {
    val startAnimator: (ClickEvent) -> Unit = {
      animator.get()?.let { it.cancel() }
      animator.set(
          ValueAnimator.ofInt(0, TimeUnit.HOURS.toMillis(12).toInt()).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { time.set((it.animatedValue as Int).toLong()) }
          })
      animator.get()?.start()
    }

    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 20f)
        .alignItems(YogaAlign.CENTER)
        .child(
            Text.create(c)
                .text("Click to Start Animation")
                .clickHandler(eventHandler(startAnimator)))
        .child(
            ClockFace.create(c)
                .time(time)
                .widthDip(200f)
                .heightDip(200f)
                .marginDip(YogaEdge.TOP, 20f)
                .build())
        .build()
  }
}
// end_example
