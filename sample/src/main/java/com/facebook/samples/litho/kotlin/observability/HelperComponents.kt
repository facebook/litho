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

package com.facebook.samples.litho.kotlin.observability

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

internal class HeaderComponent(private val name: String) : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Text(
        style = Style.margin(top = 16.dp, horizontal = 16.dp),
        text = "Hello $name, click the counter to increment it!",
        textSize = 20.sp)
  }
}

internal class CounterComponent(private val counter: Int, private val onCounterClick: () -> Unit) :
    KComponent() {

  override fun ComponentScope.render(): Component? {
    return Text(
        "Counter: $counter",
        textSize = 20.sp,
        style = Style.onClick { onCounterClick() }.padding(all = 16.dp))
  }
}

internal data class ViewState(val counter: Int, val name: String)
