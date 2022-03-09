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
import com.facebook.litho.LithoView
import com.facebook.samples.litho.NavigatableDemoActivity
import com.facebook.samples.litho.kotlin.documentation.HelloComponent

class FirstComponentActivity : NavigatableDemoActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // start_example
    setContentView(LithoView.create(this, HelloComponent(name = "Linda")))
    // end_example
  }
}
