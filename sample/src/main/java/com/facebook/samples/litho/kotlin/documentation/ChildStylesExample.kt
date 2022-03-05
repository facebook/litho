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
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlinStyle
import com.facebook.litho.view.alpha

// start_combine_style_example
class OuterTextComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    return InnerTextComponent(style = Style.margin(all = 8.dp))
  }
}

class InnerTextComponent(private val style: Style? = null) : KComponent() {
  override fun ComponentScope.render(): Component? {
    return Text(
        style = Style.padding(all = 8.dp).alpha(.5f) + style,
        text = "I accept style from a parent!")
  }
}
// end_combine_style_example
// start_combine_java_kotlin_style_example
class OuterStyleKComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    val style = Style.margin(all = 8.dp)
    return OuterStyleComponent.create(context).kotlinStyle(style).build()
  }
}
// end_combine_java_kotlin_style_example
