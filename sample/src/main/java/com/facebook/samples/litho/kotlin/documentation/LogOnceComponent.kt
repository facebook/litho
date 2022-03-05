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

package com.facebook.samples.litho.kotlin.documentation

import android.content.Context
import android.widget.Toast
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useRef
import com.facebook.litho.visibility.onVisible

// start_useref_example
class LogOnceComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val hasLoggedVisible = useRef<Boolean> { false }

    return Text(
        style =
            Style.onVisible {
              // onVisible executes on the main thread, so it's safe to read/write hasLoggedVisible
              if (!hasLoggedVisible.value) {
                doLogVisible(androidContext)
                hasLoggedVisible.value = true
              }
            },
        text = "I'll let you know when I'm visible, but only once!")
  }
}
// end_useref_example

fun doLogVisible(c: Context) {
  Toast.makeText(c, "I'm visible!", Toast.LENGTH_SHORT).show()
}
