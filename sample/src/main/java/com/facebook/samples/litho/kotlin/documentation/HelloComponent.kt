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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.view.onClick

// start_simple_example
class HelloComponent(private val name: String) : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Text(text = "Hello $name!")
  }
}
// end_simple_example

fun log(s: String) = Unit

// start_styled_example
class StyledHelloComponent(private val style: Style? = null, private val name: String) :
    KComponent() {

  override fun ComponentScope.render(): Component? {
    return Text(style = style, text = "Hello $name!")
  }
}

val componentWithOnClick =
    StyledHelloComponent(style = Style.onClick { log("clicked!") }, name = "Common Props")
// end_styled_example
