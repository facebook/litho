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

package com.facebook.samples.litho.kotlin.errors

import android.os.Bundle
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.samples.litho.NavigatableDemoActivity

class ErrorHandlingKotlinActivity : NavigatableDemoActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(
        LithoView.create(
            this, ErrorRootComponent.create(ComponentContext(this)).dataModels(DATA).build()))
  }

  companion object {
    private val DATA =
        listOf(
            ListRow("First Title", "First Subtitle"),
            ListRow("Second Title", "Second Subtitle"),
            ListRow("Third Title", "Third Subtitle"),
            ListRow("Fourth Title", "Fourth Subtitle"),
            ListRow("Fifth Title", "Fifth Subtitle"),
            ListRow("Sixth Title", "Sixth Subtitle"),
            ListRow("Seventh Title", "Seventh Subtitle"))
  }
}
