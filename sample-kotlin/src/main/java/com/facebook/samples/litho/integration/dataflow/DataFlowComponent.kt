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

package com.facebook.samples.litho.integration.dataflow

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
import com.facebook.litho.onCleanup
import com.facebook.litho.useCached
import com.facebook.litho.useEffect
import com.facebook.litho.useFlow
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/**
 * Full data flow demo: Repository -> UseCase -> ViewModel -> Component.
 *
 * The component uses `useFlow` to subscribe to the ViewModel's StateFlow<UiState> and re-renders
 * automatically when the state changes.
 */
// start_example
class DataFlowComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val viewModel = useCached { ItemViewModel() }
    val uiState = useFlow(viewModel.uiState)

    useEffect(viewModel) { onCleanup { viewModel.onDestroy() } }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Data Flow: Repository -> UseCase -> ViewModel -> Component",
              textSize = 14.sp,
              style = Style.padding(bottom = 12.dp),
          ),
      )
      child(
          when (uiState) {
            is DataFlowUiState.Loading -> {
              Column(alignItems = YogaAlign.CENTER, style = Style.padding(vertical = 32.dp)) {
                child(Progress(style = Style.width(48.dp).height(48.dp)))
                child(
                    Text(
                        text = "Loading...",
                        textSize = 14.sp,
                        style = Style.padding(top = 8.dp),
                    ),
                )
              }
            }
            is DataFlowUiState.Loaded -> {
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
            is DataFlowUiState.Empty -> {
              Column(alignItems = YogaAlign.CENTER, style = Style.padding(vertical = 32.dp)) {
                child(Text(text = "No items found", textSize = 16.sp))
              }
            }
            is DataFlowUiState.Error -> {
              Column(style = Style.padding(vertical = 16.dp)) {
                child(Text(text = "Error: ${uiState.message}", textSize = 16.sp))
                if (uiState.isRetryable) {
                  child(
                      Text(
                          text = "Tap to retry",
                          textSize = 16.sp,
                          style = Style.padding(top = 8.dp).onClick { viewModel.loadItems() },
                      ),
                  )
                }
              }
            }
          },
      )
    }
  }
}
// end_example
