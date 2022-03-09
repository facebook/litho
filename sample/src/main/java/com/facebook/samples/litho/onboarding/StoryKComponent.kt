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

import android.widget.Toast
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.drawableRes
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sp
import com.facebook.litho.view.onClick
import com.facebook.samples.litho.onboarding.model.User
import com.facebook.yoga.YogaAlign

// start_example
class StoryKComponent(private val user: User) : KComponent() {
  override fun ComponentScope.render(): Component {
    return Column(
        alignItems = YogaAlign.CENTER,
        style =
            Style.padding(all = 4.dp).onClick {
              Toast.makeText(
                      context.androidContext, "Open ${user.username} Story", Toast.LENGTH_SHORT)
                  .show()
            }) {
      child(Image(drawable = drawableRes(user.avatarRes), style = Style.width(72.dp).height(72.dp)))
      child(Text(text = user.username, textSize = 10.sp))
    }
  }
}
// end_example
