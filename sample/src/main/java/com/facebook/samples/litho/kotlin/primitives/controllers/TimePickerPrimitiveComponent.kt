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

package com.facebook.samples.litho.kotlin.primitives.controllers

import android.os.Build
import android.widget.TimePicker
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.ThreadUtils
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.utils.withEqualDimensions

class TimePickerPrimitiveComponent(
    private val controller: TimePickerController? = null,
    private val style: Style? = null
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = TimePickerLayoutBehavior,
        mountBehavior =
            // start_primitive_controller_mount_behavior_example
            MountBehavior(ViewAllocator { context -> TimePicker(context) }) {
              bind(controller) { content ->
                controller?.bind(content)
                onUnbind { controller?.unbind() }
              }
            }
        // end_primitive_controller_mount_behavior_example
        ,
        style = style)
  }
}

internal object TimePickerLayoutBehavior : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(size = Size.withEqualDimensions(sizeConstraints))
  }
}

// start_primitive_controller_code_example
class TimePickerController(private var currentHour: Int, private var currentMinute: Int) {
  var minute: Int
    get() {
      ThreadUtils.assertMainThread()
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        content?.minute ?: currentMinute
      } else {
        content?.currentMinute ?: currentMinute
      }
    }
    set(value) {
      ThreadUtils.assertMainThread()
      currentMinute = value
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        content?.minute = value
      } else {
        content?.currentMinute = value
      }
    }

  var hour: Int
    get() {
      ThreadUtils.assertMainThread()
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        content?.hour ?: currentHour
      } else {
        content?.currentHour ?: currentHour
      }
    }
    set(value) {
      ThreadUtils.assertMainThread()
      currentHour = value
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        content?.hour = value
      } else {
        content?.currentHour = value
      }
    }

  private val onTimeChangedListener =
      TimePicker.OnTimeChangedListener { _, hour, minute ->
        currentHour = hour
        currentMinute = minute
      }

  private var content: TimePicker? = null
  // end_primitive_controller_code_example

  // start_primitive_controller_bind_code_example
  fun bind(content: TimePicker) {
    this.content = content
    hour = currentHour
    minute = currentMinute
    this.content?.setOnTimeChangedListener(onTimeChangedListener)
  }

  fun unbind() {
    this.content?.setOnTimeChangedListener(null)
    this.content = null
  }
  // end_primitive_controller_bind_code_example
}
