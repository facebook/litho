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
import com.facebook.litho.Style
import com.facebook.litho.flexbox.border
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Row
import com.facebook.rendercore.dp

class AlternateColorWidthBorder : KComponent() {

  override fun ComponentScope.render(): Component {
    return Row(
        style =
            Style.border {
              top(color = NiceColor.Yellow, width = 4f.dp)
              bottom(color = NiceColor.Blue, width = 16f.dp)
              left(color = NiceColor.Red, width = 2f.dp)
              right(color = NiceColor.Green, width = 8f.dp)
            },
    ) {
      child(
          Text(
              "This component has each border specified to a different color + width",
              textSize = 20f.dp,
          ),
      )
    }
  }
}
