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

// start_sibling_sender
class PromoCodeInput(private val onPromoApplied: (discountPercent: Int) -> Unit) : KComponent() {
  override fun ComponentScope.render(): Component {
    return Text(
        text = "Apply 20% discount",
        style = Style.onClick { onPromoApplied(20) },
    )
  }
}

// end_sibling_sender

// start_sibling_receiver
class OrderSummary(private val discountPercent: Int) : KComponent() {
  override fun ComponentScope.render(): Component {
    val subtotal = 99.99
    val discount = subtotal * discountPercent / 100
    val total = subtotal - discount

    return Column {
      child(Text(text = "Subtotal: $${String.format("%.2f", subtotal)}"))
      if (discountPercent > 0) {
        child(Text(text = "Discount: -$${String.format("%.2f", discount)} ($discountPercent%)"))
      }
      child(Text(text = "Total: $${String.format("%.2f", total)}"))
    }
  }
}

// end_sibling_receiver

// start_parent_mediator
class CheckoutScreen : KComponent() {
  override fun ComponentScope.render(): Component {
    val discountPercent = useState { 0 }

    return Column {
      // PromoCodeInput sends discount via callback
      child(PromoCodeInput(onPromoApplied = { percent -> discountPercent.update { percent } }))
      // OrderSummary receives discount via prop
      child(OrderSummary(discountPercent = discountPercent.value))
    }
  }
}
// end_parent_mediator
