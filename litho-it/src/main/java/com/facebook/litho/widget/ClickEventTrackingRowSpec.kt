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

import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop

@LayoutSpec
object ClickEventTrackingRowSpec {
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop id: String,
      @Prop clickObserver: (String) -> Unit,
  ): Component {
    return Row.create(c)
        .clickHandler(ClickEventTrackingRow.onClickEvent(c))
        .widthPx(100)
        .heightPx(100)
        .viewTag("clickable")
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClickEvent(
      context: ComponentContext,
      @Prop clickObserver: (String) -> Unit,
      @Prop id: String,
  ) {
    clickObserver(id)
  }
}
