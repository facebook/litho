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

package com.facebook.samples.litho.kotlin.animations.statebased

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animation.alpha
import com.facebook.litho.animation.useAnimatedFloat
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

/**
 * Simplest state-based animation demo: tap to toggle between visible and faded.
 *
 * Demonstrates [useAnimatedFloat] + [Style.alpha]. Declare a target value, and the animation
 * happens automatically when state changes.
 */
@OptIn(ExperimentalLithoApi::class)
class FadeToggleComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val visible = useState { true }
    val alpha = useAnimatedFloat(if (visible.value) 1f else 0.3f)

    return Column(
        style = Style.padding(all = 16.dp).alpha(alpha).onClick { visible.update { !it } },
    ) {
      child(
          Column(
              style =
                  Style.height(120.dp).backgroundColor("#4285F4".toColorInt()).padding(all = 16.dp),
          ) {
            child(
                Text(
                    text = "Tap to toggle",
                    textSize = 18.sp,
                    textColor = Color.WHITE,
                ),
            )
            child(
                Text(
                    text = if (visible.value) "Visible (alpha = 1.0)" else "Faded (alpha = 0.3)",
                    textSize = 14.sp,
                    textColor = Color.WHITE,
                ),
            )
          },
      )
    }
  }
}
