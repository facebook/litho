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

package com.facebook.samples.litho.kotlin.accessibility

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.accessibility.focusOrder
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useFocusOrder
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp

class FocusOrderComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    // start_focusorder_a11y_example
    val hideElements = useState { false }
    val (first, second, third, fourth, fifth) = useFocusOrder()

    return Column(
        style = Style.padding(16.dp).contentDescription("Second hello!").focusOrder(second)) {
          child(
              Text(
                  text = "First hello!",
                  style =
                      Style.focusOrder(first).onClick {
                        hideElements.update { !hideElements.value }
                      }))

          if (hideElements.value) {
            child(Text(text = "Fifth hello!", style = Style.focusOrder(fifth)))
          }
          child(Text(text = "Fourth hello!", style = Style.focusOrder(fourth)))
          child(Text(text = "Third hello!", style = Style.focusOrder(third)))
        }
    // end_focusorder_a11y_example

  }
}
