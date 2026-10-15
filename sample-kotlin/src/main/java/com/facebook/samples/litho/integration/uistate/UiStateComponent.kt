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

package com.facebook.samples.litho.integration.uistate

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Progress
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/**
 * Demonstrates the sealed UiState pattern: a KComponent receives a UiState as a constructor prop
 * and uses exhaustive `when` to render the correct UI for each state.
 *
 * This is a self-contained demo that cycles through states on button click.
 */
class UiStateComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val stateIndex = useState { 0 }

    val sampleItems = useCached {
      listOf(
          Item(1, "First Item", "Description of first item"),
          Item(2, "Second Item", "Description of second item"),
          Item(3, "Third Item", "Description of third item"),
      )
    }

    val uiState =
        when (stateIndex.value % 4) {
          0 -> ScreenUiState.Loading
          1 -> ScreenUiState.Loaded(items = sampleItems)
          2 -> ScreenUiState.Empty
          else -> ScreenUiState.Error(message = "Something went wrong", isRetryable = true)
        }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Tap 'Next State' to cycle: Loading -> Loaded -> Empty -> Error",
              textSize = 14.sp,
              style = Style.padding(bottom = 12.dp),
          ),
      )
      child(
          Text(
              text = "Next State",
              textSize = 16.sp,
              style = Style.padding(all = 8.dp).onClick { stateIndex.update { it + 1 } },
          ),
      )
      child(ScreenContentComponent(uiState = uiState, onRetry = { stateIndex.update { 0 } }))
    }
  }
}

// start_example
/**
 * A component that renders different UI based on the sealed UiState. Uses exhaustive `when` so the
 * compiler enforces handling every state variant.
 */
class ScreenContentComponent(
    private val uiState: ScreenUiState,
    private val onRetry: () -> Unit,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    return when (uiState) {
      is ScreenUiState.Loading -> {
        Column(alignItems = YogaAlign.CENTER, style = Style.padding(vertical = 32.dp)) {
          child(Progress(style = Style.width(48.dp).height(48.dp)))
          child(Text(text = "Loading...", textSize = 14.sp, style = Style.padding(top = 8.dp)))
        }
      }
      is ScreenUiState.Loaded -> {
        Column {
          uiState.items.forEach { item ->
            child(
                Column(style = Style.padding(vertical = 8.dp)) {
                  child(Text(text = item.title, textSize = 18.sp))
                  child(
                      Text(
                          text = item.subtitle,
                          textSize = 14.sp,
                          style = Style.padding(top = 4.dp),
                      ),
                  )
                },
            )
          }
        }
      }
      is ScreenUiState.Empty -> {
        Column(alignItems = YogaAlign.CENTER, style = Style.padding(vertical = 32.dp)) {
          child(Text(text = "No items found", textSize = 16.sp))
        }
      }
      is ScreenUiState.Error -> {
        Column(style = Style.padding(vertical = 16.dp)) {
          child(Text(text = "Error: ${uiState.message}", textSize = 16.sp))
          if (uiState.isRetryable) {
            child(
                Text(
                    text = "Retry",
                    textSize = 16.sp,
                    style = Style.padding(top = 8.dp).onClick { onRetry() },
                ),
            )
          }
        }
      }
    }
  }
}
// end_example
