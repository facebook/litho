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

package com.facebook.samples.litho.kotlin.mountables.controllers

import android.content.Context
import android.os.Build
import android.widget.TimePicker
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Style
import com.facebook.litho.ThreadUtils
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.RenderState

class TimePickerComponent(
    private val controller: TimePickerController? = null,
    private val style: Style? = null
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    return MountableRenderResult(TimePickerMountable(controller), style)
  }
}
// mountable_component_start
internal class TimePickerMountable(
    private val controller: TimePickerController?,
) : SimpleMountable<TimePicker>(RenderType.VIEW) {

  override fun createContent(context: Context): TimePicker = TimePicker(context)

  override fun mount(c: Context, content: TimePicker, layoutData: Any?) {
    controller?.bind(content)
  }

  override fun unmount(c: Context, content: TimePicker, layoutData: Any?) {
    controller?.unbind(content)
  }
  // mountable_component_end

  override fun measure(
      context: RenderState.LayoutContext<*>,
      widthSpec: Int,
      heightSpec: Int
  ): MeasureResult {
    return MeasureResult.withEqualDimensions(widthSpec, heightSpec, null)
  }

  override fun shouldUpdate(
      currentMountable: SimpleMountable<TimePicker>,
      newMountable: SimpleMountable<TimePicker>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    currentMountable as TimePickerMountable
    newMountable as TimePickerMountable
    return newMountable.controller != currentMountable.controller
  }
}

// start_controller_code_example
class TimePickerController(var currentHour: Int, var currentMinute: Int) {
  private val onTimeChangedListener =
      object : TimePicker.OnTimeChangedListener {
        override fun onTimeChanged(p0: TimePicker?, hour: Int, minute: Int) {
          currentHour = hour
          currentMinute = minute
        }
      }
  var content: TimePicker? = null

  fun getMinute(): Int? {
    ThreadUtils.assertMainThread()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      content?.minute ?: currentMinute
    } else {
      content?.currentMinute ?: currentMinute
    }
  }

  fun setMinute(minute: Int) {
    ThreadUtils.assertMainThread()
    currentMinute = minute
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      content?.minute = minute
    } else {
      content?.currentMinute = minute
    }
  }
  // end_controller_code_example

  fun getHour(): Int? {
    ThreadUtils.assertMainThread()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      content?.hour ?: currentHour
    } else {
      content?.currentHour ?: currentHour
    }
  }

  fun setHour(hour: Int) {
    ThreadUtils.assertMainThread()
    currentHour = hour
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      content?.hour = hour
    } else {
      content?.currentHour = hour
    }
  }
  // start_controller_bind_code_example
  fun bind(content: TimePicker) {
    this.content = content
    setHour(currentHour)
    setMinute(currentMinute)
    setTimeChangedListener()
  }

  fun unbind(content: TimePicker) {
    this.content?.setOnTimeChangedListener(null)
    this.content = null
  }
  // end_controller_unbind_code_example

  private fun setTimeChangedListener() {
    content?.setOnTimeChangedListener(onTimeChangedListener)
  }
}
