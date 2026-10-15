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

package com.facebook.samples.litho.kotlin.navigation

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Demonstrates a bottom navigation bar pattern using Litho KComponent.
 *
 * Key patterns:
 * - Row with equal-width tabs via flex(grow = 1f)
 * - useState for tracking selected tab
 * - Conditional styling based on selected state
 * - Column with alignItems = CENTER for icon + label stacking
 * - Fixed-height bottom bar (56dp) matching material guidelines
 * - Content area that fills remaining space above the nav bar
 */
class BottomNavBarComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    return Column(style = Style.flex(grow = 1f)) { child(BottomNavBarDemo()) }
  }
}

private class BottomNavBarDemo : KComponent() {

  override fun ComponentScope.render(): Component {
    val selectedIndex = useState { 0 }
    val tabs = listOf(
        TabItem("Home", 0xFF1877F2.toInt()),
        TabItem("Search", 0xFF42B72A.toInt()),
        TabItem("Notifications", 0xFFF02849.toInt()),
        TabItem("Profile", 0xFF8B5CF6.toInt()),
    )

    return Column(style = Style.flex(grow = 1f)) {
      // Content area showing which tab is selected
      child(
          VerticalScroll(style = Style.flex(grow = 1f)) {
            Column(style = Style.padding(all = 16.dp)) {
              child(
                  Text(
                      text = "Bottom Navigation Bar Demo",
                      textSize = 24.sp,
                      textColor = 0xFF1C1E21.toInt(),
                      style = Style.margin(bottom = 16.dp),
                  ),
              )
              child(
                  Text(
                      text = "Selected tab: ${tabs[selectedIndex.value].label}",
                      textSize = 18.sp,
                      textColor = 0xFF65676B.toInt(),
                      style = Style.margin(bottom = 24.dp),
                  ),
              )
              child(
                  Text(
                      text =
                          "This demonstrates a fixed bottom navigation bar with selectable tabs. " +
                              "Each tab has an icon placeholder and label. The selected tab is highlighted " +
                              "with a different color. Tap the tabs below to switch between them.",
                      textSize = 14.sp,
                      textColor = 0xFF65676B.toInt(),
                      style = Style.margin(bottom = 16.dp),
                  ),
              )

              // Key patterns section
              child(
                  Text(
                      text = "Key Litho Patterns Used:",
                      textSize = 16.sp,
                      textColor = 0xFF1C1E21.toInt(),
                      style = Style.margin(bottom = 8.dp),
                  ),
              )

              val patterns = listOf(
                  "• Row with flex(grow = 1f) for equal tab widths",
                  "• useState for selected tab tracking",
                  "• Style.onClick for tab interaction",
                  "• Column with alignItems = CENTER for icon + label",
                  "• Fixed height (56.dp) bottom bar",
                  "• Row with backgroundColor as icon placeholder",
                  "• Conditional colors based on selection state",
              )
              patterns.forEach { pattern ->
                child(
                    Text(
                        text = pattern,
                        textSize = 13.sp,
                        textColor = 0xFF65676B.toInt(),
                        style = Style.margin(bottom = 4.dp),
                    ),
                )
              }
            }
          },
      )

      // Divider line above the nav bar
      child(Row(style = Style.height(1.dp).backgroundColor(0xFFDADDE1.toInt())))

      // Bottom Navigation Bar
      child(
          Row(
              alignItems = YogaAlign.CENTER,
              justifyContent = YogaJustify.SPACE_AROUND,
              style =
                  Style.height(56.dp)
                      .backgroundColor(0xFFFFFFFF.toInt())
                      .padding(horizontal = 8.dp),
          ) {
            tabs.forEachIndexed { index, tab ->
              child(
                  BottomNavTab(
                      label = tab.label,
                      accentColor = tab.color,
                      isSelected = selectedIndex.value == index,
                      onTabClick = { selectedIndex.update(index) },
                      style = Style.flex(grow = 1f),
                  ),
              )
            }
          },
      )
    }
  }
}

private class TabItem(val label: String, val color: Int)

private class BottomNavTab(
    private val label: String,
    private val accentColor: Int,
    private val isSelected: Boolean,
    private val onTabClick: () -> Unit,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val iconColor = if (isSelected) accentColor else 0xFF999999.toInt()
    val textColor = if (isSelected) accentColor else 0xFF999999.toInt()

    return Column(
        alignItems = YogaAlign.CENTER,
        style = style + Style.onClick { onTabClick() }.padding(vertical = 8.dp),
    ) {
      // Icon placeholder — in real usage, replace with Image(drawable =
      // drawableRes(R.drawable.icon))
      child(Row(style = Style.width(24.dp).height(24.dp).backgroundColor(iconColor)))
      child(
          Text(
              text = label,
              textSize = 12.sp,
              textColor = textColor,
              style = Style.margin(top = 4.dp),
          ),
      )
    }
  }
}
