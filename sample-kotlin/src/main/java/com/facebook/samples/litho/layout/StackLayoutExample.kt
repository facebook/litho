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

package com.facebook.samples.litho.layout

import android.graphics.Color
import android.util.Log
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.layout.StackLayout
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.SolidColor
import com.facebook.rendercore.dp
import kotlin.random.Random

/** High zIndex value used to bring components to the front of the stack. */
private const val HIGH_Z_INDEX = 999

/**
 * Example demonstrating the [StackLayout] component.
 *
 * This component displays overlapping colored boxes using [StackLayout]. Tapping anywhere on the
 * screen adds a new randomly colored box to the stack. Every other box is assigned a high zIndex
 * value to demonstrate how stacking order can be controlled.
 *
 * @see StackLayout
 * @see ColorWithEffect
 */
class StackLayoutExample : KComponent() {
  override fun ComponentScope.render(): Component {
    val colors = useState {
      listOf(Color.RED to null, Color.GREEN to HIGH_Z_INDEX, Color.BLUE to null)
    }

    return Column(style = Style.padding(15.dp).onClick { addColor(colors) }) {
      child(
          StackLayout(style = Style.viewTag("aditya").widthPercent(100f).heightPercent(100f)) {
            colors.value.forEachIndexed { index, color ->
              child(
                  component = ColorWithEffect(color.first, index),
                  zIndex = color.second ?: index,
              )
            }
          },
      )
    }
  }
}

/**
 * A colored box component that logs when it mounts.
 *
 * This component renders a [SolidColor] box and uses [useEffect] to log a message when the
 * component mounts. The box is offset by its index to create an overlapping visual effect.
 *
 * @param color The color of the box.
 * @param index The index of this box in the stack, used for offset positioning.
 */
class ColorWithEffect(private val color: Int, private val index: Int) : KComponent() {
  override fun ComponentScope.render(): Component {
    useEffect(Any()) {
      Log.d("ColorWithEffect", "ColorWithEffect index: $index")
      onCleanup {}
    }

    return SolidColor(
        color = color,
        style =
            Style.width(100.dp)
                .height(100.dp)
                .margin(top = (index * 15).dp, start = (index * 15).dp),
    )
  }
}

/**
 * Adds a new randomly colored box to the stack.
 *
 * Every other box is given a high zIndex ([HIGH_Z_INDEX]) to demonstrate stacking order control.
 *
 * @param colors The state holding the list of colors and their optional zIndex values.
 */
fun addColor(colors: State<List<Pair<Int, Int?>>>) {
  colors.update { current ->
    current +
        Pair(
            Color.rgb(
                Random.nextInt(256),
                Random.nextInt(256),
                Random.nextInt(256),
            ),
            if (current.size % 2 == 0) null else HIGH_Z_INDEX,
        )
  }
}
