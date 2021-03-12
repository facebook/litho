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

import android.graphics.Typeface
import android.os.Bundle
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.dp
import com.facebook.litho.flexbox.padding
import com.facebook.litho.setContent
import com.facebook.litho.sp
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.NavigatableDemoActivity

class PlaygroundActivity : NavigatableDemoActivity() {

  class PlaygroundComponent : KComponent() {
    override fun ComponentScope.render(): Component? {
      val counter = useState { 1 }

      return Column(
          style = Style.padding(16.dp).onClick { counter.update { value -> value + 1 } },
          children =
              listOf(
                  Text(text = "Hello, Kotlin World!", textSize = 20.sp),
                  Text(
                      text = "with ${"❤️".repeat(counter.value)} from London",
                      textStyle = Typeface.ITALIC),
              ))
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent(PlaygroundComponent())
  }
}
