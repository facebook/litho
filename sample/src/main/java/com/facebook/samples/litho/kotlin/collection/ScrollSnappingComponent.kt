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
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.view.background
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.widget.SnapUtil.SNAP_TO_START
import com.facebook.litho.widget.collection.LazyList
import com.facebook.litho.widget.collection.LinearSpacing
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class ScrollSnappingComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    return Column {
      child(HorizontalScroll())
      child(Row(style = Style.height(2.dp).backgroundColor(Color.BLACK)))
      child(VerticalScroll())
    }
  }
}

class HorizontalScroll : KComponent() {

  override fun ComponentScope.render(): Component =
      LazyList(
          snapMode = SNAP_TO_START,
          snapToStartOffset = 20.dp,
          orientation = RecyclerView.HORIZONTAL,
          itemDecoration = LinearSpacing(all = 10.dp),
          style = Style.height(100.dp),
      ) {
        children(items = (0..20), id = { it }) { ScrollItem("$it") }
      }
}

class VerticalScroll : KComponent() {

  override fun ComponentScope.render(): Component =
      LazyList(
          snapMode = SNAP_TO_START,
          snapToStartOffset = 20.dp,
          itemDecoration = LinearSpacing(all = 10.dp),
          style = Style.flex(grow = 1f),
      ) {
        children(items = (0..20), id = { it }) { ScrollItem("$it") }
      }
}

class ScrollItem(private val text: String, private val style: Style? = null) : KComponent() {
  override fun ComponentScope.render(): Component =
      Text(
          text = text,
          textSize = 24.sp,
          style = Style.padding(all = 30.dp).background(RoundedRect(Color.LTGRAY, 10.dp)) + style)
}
