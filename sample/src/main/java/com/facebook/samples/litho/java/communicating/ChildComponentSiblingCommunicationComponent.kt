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
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.border
import com.facebook.litho.kotlin.widget.Border
import com.facebook.litho.kotlin.widget.BorderEdge
import com.facebook.litho.kotlin.widget.BorderRadius
import com.facebook.litho.kotlin.widget.SolidColor
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp

// start_dispatch_to_parent
class ChildComponentSiblingCommunicationComponent(
    private val id: Int,
    private val isSelected: Boolean,
    private val onSelected: (Int) -> Unit,
) : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Row(style = Style.onClick { onSelected(id) }.margin(all = 30.dp)) {
      child(
          SolidColor(
              color = if (isSelected) Color.BLUE else Color.WHITE,
              style =
                  Style.width(20.dp)
                      .height(20.dp)
                      .margin(top = 10.dp, end = 30.dp)
                      .border(
                          Border(
                              edgeAll = BorderEdge(color = Color.BLUE, width = 1.dp),
                              radius = BorderRadius(all = 2.dp)))))
      child(Text(text = "ChildComponent $id", textSize = 20.dp))
    }
  }
}
// end_dispatch_to_parent
