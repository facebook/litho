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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.rendercore.dp

// start_parent_mediator
class ParentComponentMediatorKComponent : KComponent() {
  override fun ComponentScope.render(): Component? {

    val selectedPosition = useState { 0 }

    return Column(style = Style.padding(all = 30.dp)) {
      child(Text(text = "ChildComponent", textSize = 30.dp))
      child(
          ChildComponentSiblingCommunicationKComponent(
              id = 0,
              isSelected = selectedPosition.value == 0,
              onSelected = { selectedPosition.update(0) }))
      child(
          ChildComponentSiblingCommunicationKComponent(
              id = 1,
              isSelected = selectedPosition.value == 1,
              onSelected = { selectedPosition.update(1) }))
    }
  }
}
// end_parent_mediator
