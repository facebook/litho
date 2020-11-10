/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.samples.litho.kotlin.demo

import android.content.Intent
import android.view.View
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge.ALL

@LayoutSpec
object DemoListItemComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop model: DemoListDataModel): Component =
      Column.create(c)
          .paddingDip(ALL, 16f)
          .child(Text.create(c).text(model.name).textSizeSp(18f).build())
          .clickHandler(DemoListItemComponent.onClick(c))
          .build()

  @OnEvent(ClickEvent::class)
  fun onClick(
      c: ComponentContext,
      @FromEvent view: View,
      @Prop model: DemoListDataModel,
      @Prop currentIndices: IntArray
  ) {
    val activityClass =
        if (model.datamodels == null) {
          model.klass
        } else {
          DemoListActivity::class.java
        }

    val intent = Intent(c.getAndroidContext(), activityClass)
    intent.putExtra(DemoListActivity.INDICES, currentIndices)
    c.getAndroidContext().startActivity(intent)
  }
}
