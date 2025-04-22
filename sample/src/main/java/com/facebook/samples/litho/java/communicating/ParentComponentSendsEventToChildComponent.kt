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

package com.facebook.samples.litho.java.communicating

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.flexbox.border
import com.facebook.litho.kotlin.widget.Border
import com.facebook.litho.kotlin.widget.BorderEdge
import com.facebook.litho.kotlin.widget.BorderRadius
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp

class ParentComponentSendsEventToChildComponent : KComponent() {

  // start_define_controller
  override fun ComponentScope.render(): Component? {

    val controller = useCached { ParentToChildEventController() }
    val version = useState { 0 }

    return Column(style = Style.padding(all = 30.dp)) {
      child(Text(text = "ParentComponent", textSize = 30.dp))
      child(
          Text(
              text = "Click to trigger show toast event on ChildComponent",
              textSize = 15.dp,
              style =
                  Style.padding(all = 5.dp)
                      .margin(top = 15.dp)
                      .border(
                          Border(
                              edgeAll = BorderEdge(color = Color.BLACK, width = 1.dp),
                              radius = BorderRadius(all = 2.dp)))
                      .onClick { controller.trigger("Message from parent") }))
      child(
          ChildComponentReceivesEventFromParentComponent(
              controller = controller, textFromParent = "Child with controller"))
      // end_define_controller
      // start_update_prop
      child(
          Text(
              text = "Click to send new text to ChildComponent",
              textSize = 15.dp,
              style =
                  Style.padding(all = 5.dp)
                      .margin(top = 15.dp)
                      .border(
                          Border(
                              edgeAll = BorderEdge(color = Color.BLACK, width = 1.dp),
                              radius = BorderRadius(all = 2.dp)))
                      .onClick { version.update { it + 1 } }))
      child(
          ChildComponentReceivesEventFromParentComponent(
              controller = controller, textFromParent = "Version ${version.value}"))
      // end_update_prop
    }
  }
}
