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

package com.facebook.samples.litho.documentation.flexbox

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.widthPercent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

class TextInsideContainer : KComponent() {

  // start_example
  override fun ComponentScope.render(): Component {
    return Column(style = Style.height(135.dp).padding(16.dp)) {
      child(
          Column(style = Style.heightPercent(100f).widthPercent(100f)) {
            // either remove widthPercent / heightPercent here so that both siblings can grow
            // equally
            child(Text(text = "This is a really long text.", textSize = 20.sp, maxLines = 1))
            child(
                Text(
                    style = Style.margin(top = 8.dp),
                    text = "Subtitle text",
                    textSize = 20.sp,
                    maxLines = 1))
          })
      child(
          Text(
              // or add flex(shrink = 0f) so that this component does not shrink
              style = Style.margin(top = 16.dp),
              text = "Small footer text",
              textSize = 20.sp,
              maxLines = 1))
    }
  }
  // end_example
}
