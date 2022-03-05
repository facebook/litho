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

package com.facebook.samples.litho.kotlin.bordereffects

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.dp
import com.facebook.litho.flexbox.border
import com.facebook.litho.kotlin.widget.Border
import com.facebook.litho.kotlin.widget.BorderEdge
import com.facebook.litho.kotlin.widget.BorderEffect
import com.facebook.litho.kotlin.widget.BorderRadius
import com.facebook.litho.kotlin.widget.Text

// this doesn't actually have a path on it though?
class AlternateColorCornerPathEffectBorder : KComponent() {

  override fun ComponentScope.render(): Component {
    return Row(
        style =
            Style.border(
                Border(
                    edgeAll = BorderEdge(width = 5f.dp),
                    edgeTop = BorderEdge(color = NiceColor.ORANGE),
                    edgeBottom = BorderEdge(color = NiceColor.BLUE),
                    edgeLeft = BorderEdge(color = NiceColor.RED),
                    edgeRight = BorderEdge(color = NiceColor.GREEN),
                    radius = BorderRadius(20f.dp),
                    effect = BorderEffect.discrete(4f, 11f)))) {
      child(
          Text(
              "This component has a path effect with rounded corners + multiple colors",
              textSize = 20f.dp))
    }
  }
}
