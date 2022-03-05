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
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlinStyle

@MountSpec
object SeekBarSpec {

  private const val MAX = 256

  @OnCreateMountContent
  fun onCreateMountContent(c: Context): android.widget.SeekBar {
    return android.widget.SeekBar(c)
  }

  @OnMount
  fun onMount(
      c: ComponentContext,
      seekBar: android.widget.SeekBar,
      @Prop initialValue: Float = 1f,
      @Prop onProgressChanged: (Float) -> Unit,
  ) {
    seekBar.max = MAX
    seekBar.progress = (initialValue * MAX).toInt()
    seekBar.setOnSeekBarChangeListener(
        object : android.widget.SeekBar.OnSeekBarChangeListener {
          override fun onProgressChanged(
              seekBar: android.widget.SeekBar,
              progress: Int,
              fromUser: Boolean
          ) {
            onProgressChanged(progress / MAX.toFloat())
          }

          override fun onStartTrackingTouch(seekBar: android.widget.SeekBar) = Unit

          override fun onStopTrackingTouch(seekBar: android.widget.SeekBar) = Unit
        })
  }

  @OnUnmount
  fun onUnmount(
      c: ComponentContext,
      seekBar: android.widget.SeekBar,
  ) {
    seekBar.setOnSeekBarChangeListener(null)
  }
}

fun ResourcesScope.SeekBar(
    initialValue: Float = 1f,
    label: CharSequence? = null,
    style: Style? = null,
    onProgressChanged: (Float) -> Unit,
): Component = Row {
  label?.let { child(Text(label, style = Style.margin(end = 10.dp))) }
  child(
      SeekBar.create(context)
          .initialValue(initialValue)
          .onProgressChanged { progress -> onProgressChanged(progress) }
          .kotlinStyle(Style.height(14.dp).flex(grow = 1f) + style)
          .build())
}
