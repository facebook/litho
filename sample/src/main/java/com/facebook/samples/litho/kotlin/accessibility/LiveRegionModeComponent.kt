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

import android.graphics.Color
import android.view.View
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.accessibility.liveRegion
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.dp
import com.facebook.samples.litho.kotlin.collection.Button

class LiveRegionModeComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val mode = useState { View.ACCESSIBILITY_LIVE_REGION_NONE }
    val text = useState { "A" }

    return Column(style = Style.padding(16.dp)) {
      child(
          Column(style = Style.backgroundColor(Color.LTGRAY)) {
            child(
                Text(
                    text = "Live Region ${getModeName(mode.value)}. Text: ${text.value}",
                    style = Style.liveRegion(mode.value)))

            child(Button("Update Text") { text.update { if (it == "A") "B" else "A" } })
            child(
                Button("Update Mode") {
                  mode.update {
                    when (it) {
                      View.ACCESSIBILITY_LIVE_REGION_NONE -> View.ACCESSIBILITY_LIVE_REGION_POLITE
                      View.ACCESSIBILITY_LIVE_REGION_POLITE ->
                          View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
                      View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE ->
                          View.ACCESSIBILITY_LIVE_REGION_NONE
                      else -> View.ACCESSIBILITY_LIVE_REGION_NONE
                    }
                  }
                })
          })
    }
  }

  private fun getModeName(mode: Int): String {
    return when (mode) {
      View.ACCESSIBILITY_LIVE_REGION_NONE -> "NONE"
      View.ACCESSIBILITY_LIVE_REGION_POLITE -> "POLITE"
      View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE -> "ASSERTIVE"
      else -> "UNKNOWN"
    }
  }
}
