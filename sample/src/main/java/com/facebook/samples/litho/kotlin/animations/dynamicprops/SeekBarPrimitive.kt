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

import android.widget.SeekBar
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.rendercore.primitives.EqualDimensionsLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator

class SeekBarPrimitive(
    private val onProgressChange: (Float) -> Unit,
    private val initialValue: Float = 0f,
    private val style: Style? = null
) : PrimitiveComponent() {

  private val MAX = 256

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = EqualDimensionsLayoutBehavior,
        mountBehavior =
            MountBehavior(ViewAllocator { context -> SeekBar(context) }) {
              bind(initialValue) { content ->
                val defaultMax = content.max
                content.max = MAX
                content.progress = (initialValue * MAX).toInt()
                onUnbind {
                  content.max = defaultMax
                  content.progress = 0
                }
              }

              bind(onProgressChange) { content ->
                content.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                      override fun onProgressChanged(
                          seekBar: SeekBar,
                          progress: Int,
                          fromUser: Boolean
                      ) {
                        onProgressChange(progress / MAX.toFloat())
                      }

                      override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                      override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
                    })
                onUnbind { content.setOnSeekBarChangeListener(null) }
              }
            },
        style = style)
  }
}
