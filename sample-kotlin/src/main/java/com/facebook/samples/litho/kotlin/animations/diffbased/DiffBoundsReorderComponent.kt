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

package com.facebook.samples.litho.kotlin.animations.diffbased

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.animation.DampingRatio
import com.facebook.litho.animation.Stiffness
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.transition
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.key
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/** Reorders keyed items and animates their mount-space bounds. */
@OptIn(ExperimentalLithoApi::class)
class DiffBoundsReorderComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val reversed = useState { false }
    val items =
        useCached(reversed.value) {
          if (reversed.value) REORDER_ITEMS.reversed() else REORDER_ITEMS
        }

    return Column(
        style = Style.padding(all = 24.dp).onClick { reversed.update { !it } },
    ) {
      child(
          Text(
              text = "Tap to reorder",
              textSize = 16.sp,
              textColor = Color.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      child(
          Column(style = Style.width(340.dp).height(380.dp)) {
            for (item in items) {
              child(reorderRow(item))
            }
          },
      )
    }
  }

  private fun ResourcesScope.reorderRow(item: ReorderItem): Component =
      key(item.id) {
        Row(
            alignItems = YogaAlign.CENTER,
            style =
                Style.width(if (item.wide) 320.dp else 268.dp)
                    .height(58.dp)
                    .margin(bottom = 12.dp)
                    .backgroundColor(item.color)
                    .padding(horizontal = 18.dp)
                    .transition {
                      bounds =
                          spring(
                              stiffness = Stiffness.Medium,
                              dampingRatio = DampingRatio.MediumBouncy,
                          )
                    },
        ) {
          child(
              Text(
                  text = item.label,
                  textSize = 17.sp,
                  textColor = Color.WHITE,
              ),
          )
        }
      }
}

private class ReorderItem(
    val id: String,
    val label: String,
    val color: Int,
    val wide: Boolean,
)

private val REORDER_ITEMS = listOf(
    ReorderItem("design", "Design review", "#4285F4".toColorInt(), true),
    ReorderItem("prototype", "Prototype", "#34A853".toColorInt(), false),
    ReorderItem("qa", "QA signoff", "#FBBC04".toColorInt(), true),
    ReorderItem("docs", "Docs", "#9C27B0".toColorInt(), false),
    ReorderItem("ship", "Ship", "#EA4335".toColorInt(), true),
)
