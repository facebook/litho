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

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animated.alpha
import com.facebook.litho.animated.backgroundColor
import com.facebook.litho.animated.elevation
import com.facebook.litho.animated.foregroundColor
import com.facebook.litho.animated.rotation
import com.facebook.litho.animated.scaleX
import com.facebook.litho.animated.scaleY
import com.facebook.litho.animated.translationX
import com.facebook.litho.animated.useBinding
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.flexbox.alignSelf
import com.facebook.yoga.YogaAlign

class AllCommonDynamicPropsKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val scale = useBinding(1f)
    val alpha = useBinding(1f)
    val translation = useBinding(1f)
    val rotation = useBinding(0f)
    val elevation = useBinding(1f)

    val bgColor = useBinding(Color.RED)
    val fgColor = useBinding(Color.GREEN)

    val square =
        Column(
            style =
                Style.width(100.dp)
                    .height(100.dp)
                    .foregroundColor(fgColor)
                    .backgroundColor(bgColor)
                    .alignSelf(YogaAlign.CENTER)
                    .scaleX(scale)
                    .scaleY(scale)
                    .alpha(alpha)
                    .translationX(translation)
                    .rotation(rotation)
                    .elevation(elevation))

    return Column(style = Style.padding(all = 20.dp)) {
      child(SeekBar(initialValue = 1f, label = "Alpha") { alpha.set(it) })
      child(
          SeekBar(
              initialValue = .5f,
              label = "Scale",
              onProgressChanged = { scale.set(evaluate(it, .75f, 1.25f)) }))
      child(
          SeekBar(
              initialValue = .5f,
              label = "Translation",
              onProgressChanged = { translation.set(evaluate(it, -100f, 100f)) }))
      child(
          SeekBar(
              initialValue = 0f,
              label = "Rotation",
              onProgressChanged = { rotation.set(evaluate(it, 0f, 360f)) }))
      child(
          SeekBar(
              initialValue = 0f,
              label = "background Color",
              onProgressChanged = {
                bgColor.set(Color.HSVToColor(floatArrayOf(evaluate(it, 0f, 360f), 1f, 1f)))
              }))
      child(
          SeekBar(
              initialValue = 0f,
              label = "foreground Color",
              onProgressChanged = {
                fgColor.set(Color.HSVToColor(floatArrayOf(evaluate(it, 0f, 360f), 1f, 1f)))
              }))
      child(
          SeekBar(
              initialValue = 0f,
              label = "Elevation",
              onProgressChanged = { elevation.set(evaluate(it, 0f, 10f)) }))

      child(square)
    }
  }

  fun evaluate(fraction: Float, start: Float, end: Float): Float = start + fraction * (end - start)
}
