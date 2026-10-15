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

package com.facebook.litho.kotlin.widget

import android.graphics.Color
import androidx.annotation.ColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.maxHeight
import com.facebook.litho.core.minHeight
import com.facebook.litho.drawableColor
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.flex
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.background
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign

/**
 * A horizontal divider that expands along its width while preserving its [thickness].
 *
 * @param color Color of the divider
 * @param thickness Thickness of the divider
 * @param style Additional Style applied before the divider's thickness and alignment
 */
class HorizontalDivider(
    @ColorInt private val color: Int = Color.GRAY,
    private val thickness: Dimen = 1.dp,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    return Row(
        style =
            style +
                Style.height(thickness)
                    .minHeight(thickness)
                    .maxHeight(thickness)
                    .alignSelf(YogaAlign.STRETCH)
                    .flex(grow = 1f)
                    .background(drawableColor(color)),
    )
  }
}
