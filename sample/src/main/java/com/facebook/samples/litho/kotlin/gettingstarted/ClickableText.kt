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

package com.facebook.samples.litho.kotlin.gettingstarted

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewId
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.samples.litho.R

/**
 * Text component which will display a greeting with the given name.
 *
 * @param name the name to display in the greeting message
 * @constructor Instantiates a new BootcampText component
 * @property name the name that will be displayed in the greeting message
 */
class ClickableText(private val name: String) : KComponent() {
  override fun ComponentScope.render(): Component {
    val clickCounter = useState { 0 }

    return Column {
      child(
          Text(
              style = Style.margin(top = 16.dp, horizontal = 16.dp),
              text = "Hello $name!",
              textSize = 20.sp,
          ))
      child(
          Text(
              style =
                  Style.onClick { clickCounter.update { it + 1 } }
                      .viewId(R.id.sample)
                      .padding(all = 16.dp),
              text = "I have been clicked ${clickCounter.value} times.",
              textSize = 20.sp,
          ))
    }
  }
}
