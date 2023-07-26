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

import android.widget.Toast
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.useCached
import com.facebook.rendercore.dp
import com.facebook.samples.litho.kotlin.collection.Button

class ControllersExamplePrimitiveComponent : KComponent() {
  // start_primitive_controllers_example
  override fun ComponentScope.render(): Component {
    return Column(style = Style.padding(16.dp)) {
      val initialHour = 14
      val initialMinute = 30
      val controller =
          useCached(initialHour, initialMinute) { TimePickerController(initialHour, initialMinute) }
      child(TimePickerPrimitiveComponent(controller = controller))
      child(
          Button(
              text = "Set random time ",
              onClick = {
                controller.hour = (0..24).random()
                controller.minute = (0..60).random()
              }))
      // end_primitive_controllers_example
      child(
          Button(
              text = "Get current time",
              onClick = {
                Toast.makeText(
                        androidContext,
                        String.format("%02d:%02d", controller.hour, controller.minute),
                        Toast.LENGTH_SHORT)
                    .show()
              }))
    }
  }
}
