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

import android.content.Context
import android.widget.SeekBar
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Style
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.RenderState

class SeekBarMountable(
    private val onProgressChange: (Float) -> Unit,
    private val initialValue: Float = 0f,
    private val style: Style? = null
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    return MountableRenderResult(Mountable(onProgressChange, initialValue), style)
  }

  private class Mountable(
      private val onProgressChange: (Float) -> Unit,
      private val initialValue: Float
  ) : SimpleMountable<SeekBar>(RenderType.VIEW) {

    private val MAX = 256

    override fun createContent(context: Context): SeekBar = SeekBar(context)

    override fun mount(c: Context, content: SeekBar, layoutData: Any?) {
      content.max = MAX
      content.progress = (initialValue * MAX).toInt()
      content.setOnSeekBarChangeListener(
          object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
              onProgressChange(progress / MAX.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
          })
    }

    override fun unmount(c: Context, content: SeekBar, layoutData: Any?) {
      content.setOnSeekBarChangeListener(null)
    }

    override fun measure(
        context: RenderState.LayoutContext<*>,
        widthSpec: Int,
        heightSpec: Int,
        previousLayoutData: Any?
    ): MeasureResult = MeasureResult.fromSpecs(widthSpec, heightSpec)
  }
}
