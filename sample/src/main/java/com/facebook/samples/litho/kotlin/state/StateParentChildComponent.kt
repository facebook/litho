/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.samples.litho.kotlin.state

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.dp
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.sp
import com.facebook.litho.useState
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextAlignment
import com.facebook.yoga.YogaAlign

// start_example_parent
class StateParentChildComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val clicks = useState { 0 }
    return Column {
      child(ChildComponent { clickNumber -> clicks.update { c -> c + clickNumber } })
      child(
          Text(
              style = Style.alignSelf(YogaAlign.CENTER).margin(vertical = 16.dp),
              text = "Counter: ${clicks.value}",
              alignment = TextAlignment.CENTER,
              textSize = 14.sp))
    }
  }
}
// end_example_parent
