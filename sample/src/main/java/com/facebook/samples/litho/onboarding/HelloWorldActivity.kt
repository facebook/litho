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
package com.facebook.samples.litho.onboarding

import android.os.Bundle
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.NavigatableDemoActivity

class HelloWorldActivity : NavigatableDemoActivity() {
  // start_example
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val c = ComponentContext(this)
    setContentView(LithoView.create(this, Text.create(c).text("Hello World!").build()))
  }
  // end_example
}
