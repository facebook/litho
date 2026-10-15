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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.TextInput
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.useStateWithDeps
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp

// start_example
/**
 * Example demonstrating useStateWithDeps.
 *
 * This component shows a search box where the search result message is reset whenever the search
 * text changes. The key difference from useState is that useStateWithDeps re-initializes the state
 * value when any of its dependencies change.
 *
 * In this example:
 * - searchText is a dependency for resultMessage
 * - When searchText changes, resultMessage is automatically reset to empty string
 * - This happens without an explicit update call
 * - The re-initialization itself doesn't trigger a re-render
 */
@OptIn(ExperimentalLithoApi::class)
class UseStateWithDepsExampleComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    // Regular state for search text
    val searchText = useState { "" }

    // State with dependency: automatically resets when searchText changes
    // This is useful for clearing derived state when inputs change
    val resultMessage = useStateWithDeps(searchText.value) { "" }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Search with Auto-Reset Result",
              textSize = 20.dp,
              style = Style.margin(bottom = 16.dp),
          ),
      )

      // Search input
      child(
          TextInput(
              initialText = searchText.value,
              textSize = 16.dp,
              hint = "Type to search...",
              inputBackground = null,
              onTextChanged = { event -> searchText.update(event.text) },
              style = Style.height(48.dp).margin(bottom = 8.dp),
          ),
      )

      // Search button that sets result message
      child(
          Row(style = Style.margin(bottom = 16.dp)) {
            child(
                Text(
                    text = "Search",
                    textSize = 16.dp,
                    style =
                        Style.padding(all = 12.dp).margin(end = 8.dp).onClick {
                          // When search is clicked, show result
                          // If user then changes searchText, resultMessage will auto-reset to ""
                          resultMessage.update("Results for: \"${searchText.value}\"")
                        },
                ),
            )
            child(
                Text(
                    text = "Clear",
                    textSize = 16.dp,
                    style =
                        Style.padding(all = 12.dp).onClick {
                          // Clear search text, which also auto-resets resultMessage
                          searchText.update("")
                        },
                ),
            )
          },
      )

      // Display result message (resets automatically when searchText changes)
      child(
          Text(
              text =
                  if (resultMessage.value.isEmpty()) {
                    "Type and click Search. Result will reset when you change the text."
                  } else {
                    resultMessage.value
                  },
              textSize = 14.dp,
              style = Style.margin(top = 8.dp),
          ),
      )

      // Explanation text
      child(
          Text(
              text =
                  "\n\nHow it works:\n" +
                      "• resultMessage depends on searchText.value\n" +
                      "• When searchText changes, resultMessage resets to \"\"\n" +
                      "• No explicit reset call needed\n" +
                      "• Try: Click Search, see result, then type more text",
              textSize = 12.dp,
              style = Style.margin(top = 16.dp),
          ),
      )
    }
  }
}
// end_example
