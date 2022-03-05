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

package com.facebook.samples.litho.kotlin.collection

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.widget.collection.LazyCollectionController
import com.facebook.litho.widget.collection.LazyList

// start_example
class PullToRefreshCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val deck = useState { (0..51).map { Card(it) }.shuffled() }
    val lazyCollectionController = useState { LazyCollectionController() }.value
    return LazyList(
        lazyCollectionController = lazyCollectionController,
        onPullToRefresh = {
          deck.update { it.shuffled() }
          lazyCollectionController.clearRefreshing()
        },
    ) { deck.value.forEach { card -> child(id = card.index, component = Text(card.styledText)) } }
  }
}
// end_example

class Card(val suit: Suit, val value: Int) {

  enum class Suit(val symbol: Char, @ColorInt val color: Int) {
    SPADES('♠', Color.BLACK),
    HEARTS('♥', Color.RED),
    CLUBS('♣', Color.BLACK),
    DIAMONDS('♦', Color.RED),
  }

  constructor(index: Int) : this(Suit.values()[index / 13], (index % 13) + 1)

  init {
    if (value !in 1..13) {
      throw IllegalArgumentException()
    }
  }

  val valueText
    get() =
        when (value) {
          1 -> "A"
          11 -> "J"
          12 -> "Q"
          13 -> "K"
          else -> "$value"
        }

  val styledText
    get(): CharSequence {
      val spannable = SpannableString("$valueText ${suit.symbol}")
      spannable.setSpan(
          ForegroundColorSpan(suit.color), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
      return spannable
    }

  val index
    get() = (suit.ordinal * 13) + (value - 1)
}
