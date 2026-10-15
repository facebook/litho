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

package com.facebook.litho.widget

import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.view.onClick

class OnClickCallbackComponent(
    private val callback: View.OnClickListener,
    private val style: Style? = null,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val combinedStyle = Style.onClick { onClick(callback, it.view) } + style
    return Column(style = combinedStyle) { child(Text.create(context).text("hello world").build()) }
  }

  private fun ResourcesScope.onClick(callback: View.OnClickListener, view: View) {
    callback.onClick(view)
  }
}
