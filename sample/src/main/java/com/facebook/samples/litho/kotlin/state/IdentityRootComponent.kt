// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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

package com.facebook.samples.litho.kotlin.state

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.dp
import com.facebook.litho.key
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useRef
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.visibility.onVisible

class IdentityRootComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val isFirstCounterEnabled = useState { true }
    val isSecondCounterEnabled = useState { true }
    // start_use_ref
    val logOnce = useRef { false }
    // start_manual_key
    return Column(
        style =
            Style.onVisible {
              if (!logOnce.value) {
                // do some logging
                logOnce.value = true
              }
            }) { // end_use_ref
      if (isFirstCounterEnabled.value) {
        child(
            key("first_row") {
              Row {
                child(CounterComponent())
                child(
                    Text(
                        text = "X",
                        textSize = 30.dp,
                        style =
                            Style.margin(all = 30.dp).onClick {
                              isFirstCounterEnabled.update(false)
                            }))
              }
            })
      }
      // end_manual_key
      if (isSecondCounterEnabled.value) {
        child(
            key("second_row") {
              Row {
                child(CounterComponent())
                child(
                    Text(
                        text = "X",
                        textSize = 30.dp,
                        style =
                            Style.margin(all = 30.dp).onClick {
                              isSecondCounterEnabled.update(false)
                            }))
              }
            })
      }
    }
  }
}
