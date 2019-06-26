/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.codelab.events

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.InvisibleEvent
import com.facebook.litho.LongClickEvent
import com.facebook.litho.Row
import com.facebook.litho.VisibleEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaWrap

@Suppress("MagicNumber")
@LayoutSpec(events = [BoxItemChangedEvent::class])
object ColorBoxCollectionSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop items: IntArray, @Prop highlightedIndex: Int): Component {
    val rowBuilder = Row.create(c).wrap(YogaWrap.WRAP)
    items.forEachIndexed { index, color ->
      val isHighlighted = index == highlightedIndex
      rowBuilder.child(
          Row.create(c)
              .marginDip(YogaEdge.ALL, 5f)
              .widthDip(50f * if (isHighlighted) 1.2f else 1f)
              .heightDip(50f * if (isHighlighted) 1.2f else 1f)
              .backgroundColor(color)
              .longClickHandler(ColorBoxCollection.onLongClick(c, color, index))
              .visibleHandler(ColorBoxCollection.onItemVisible(c, color, index))
              .invisibleHandler(ColorBoxCollection.onItemInvisible(c, color, index)))
    }
    return rowBuilder.build()
  }

  @OnEvent(VisibleEvent::class)
  fun onItemVisible(c: ComponentContext, @Param color: Int, @Param index: Int) {
    ColorBoxCollection.dispatchBoxItemChangedEvent(
        ColorBoxCollection.getBoxItemChangedEventHandler(c),
        color,
        "Item at index $index is now visible",
        -1
    )
  }

  @OnEvent(InvisibleEvent::class)
  fun onItemInvisible(c: ComponentContext, @Param color: Int, @Param index: Int) {
    ColorBoxCollection.dispatchBoxItemChangedEvent(
        ColorBoxCollection.getBoxItemChangedEventHandler(c),
        color,
        "Item at index $index is no longer visible",
        -1
    )
  }

  @OnEvent(LongClickEvent::class)
  fun onLongClick(c: ComponentContext, @Param color: Int, @Param index: Int): Boolean {
    ColorBoxCollection.dispatchBoxItemChangedEvent(
        ColorBoxCollection.getBoxItemChangedEventHandler(c),
        color,
        "Item at index $index was highlighted",
        index)
    return true
  }
}
