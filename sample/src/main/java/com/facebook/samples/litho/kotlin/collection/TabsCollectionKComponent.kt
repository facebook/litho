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
import android.graphics.Typeface
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Row
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.colorRes
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sp
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.TextAlignment
import com.facebook.litho.widget.collection.LazyList
import com.facebook.samples.litho.R

class TabsCollectionKComponent : KComponent() {

  enum class Tab(val title: String) {
    Tab1("Tab1"),
    Tab2("Tab2"),
    Tab3("Tab3"),
  }

  override fun ComponentScope.render(): Component {
    val selectedTab = useState { Tab.Tab1 }

    return LazyList {
      child(tabBar(selectedTab))

      when (selectedTab.value) {
        Tab.Tab1 -> {
          child(id = "tab1_tile", component = Text("Tab1 Title"))
          child(id = "tab1_subtitle", component = Text("Tab1 Subtitle"))
        }
        Tab.Tab2 -> {
          child(id = "tab2_tile", component = Text("Tab2 Title"))
          child(id = "tab2_subtitle", component = Text("Tab2 Subtitle"))
        }
        Tab.Tab3 -> {
          child(id = "tab3_tile", component = Text("Tab3 Title"))
          child(id = "tab3_subtitle", component = Text("Tab3 Subtitle"))
        }
      }
    }
  }

  private fun ResourcesScope.tabBar(selectedTab: State<Tab>): Component = Row {
    Tab.values().forEach { tab ->
      val isSelected = tab == selectedTab.value
      child(
          Text(
              tab.title.uppercase(),
              textSize = 16.sp,
              alignment = TextAlignment.CENTER,
              textColor = if (isSelected) colorRes(R.color.primaryColor) else Color.DKGRAY,
              textStyle = if (isSelected) Typeface.BOLD else Typeface.NORMAL,
              style =
                  Style.padding(all = 16.dp)
                      .flex(grow = 1f)
                      .backgroundColor(
                          if (isSelected) colorRes(R.color.colorPrimaryLightBg) else Color.WHITE)
                      .onClick { selectedTab.update { tab } }))
    }
  }
}
