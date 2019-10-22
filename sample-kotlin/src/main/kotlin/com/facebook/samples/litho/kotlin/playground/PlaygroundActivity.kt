/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.samples.litho.kotlin.playground

import android.os.Bundle
import com.facebook.litho.Column
import com.facebook.litho.setContent
import com.facebook.litho.sp
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.NavigatableDemoActivity

class PlaygroundActivity : NavigatableDemoActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      Column {
        +Text(text = "Hello, Kotlin World!", textSize = 20.sp)
        +Text(text = "with ❤️ from London")
      }
    }
  }
}
