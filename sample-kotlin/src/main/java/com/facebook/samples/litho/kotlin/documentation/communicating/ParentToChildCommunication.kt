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

package com.facebook.samples.litho.kotlin.documentation.communicating

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useState
import com.facebook.litho.view.onClick

// start_parent_updates_child
class CartScreen : KComponent() {
  override fun ComponentScope.render(): Component {
    val totalPrice = useState { 0.0 }

    return Column {
      child(Text(text = "Your Shopping Cart"))
      // When clicked, add an item and update the total
      child(
          Text(
              text = "Add $29.99 item",
              style = Style.onClick { totalPrice.update { it + 29.99 } },
          ),
      )
      // CartTotal receives the current total as a prop
      child(CartTotal(total = totalPrice.value))
    }
  }
}

class CartTotal(private val total: Double) : KComponent() {
  override fun ComponentScope.render(): Component {
    return Text(text = "Total: $${String.format("%.2f", total)}")
  }
}
// end_parent_updates_child
