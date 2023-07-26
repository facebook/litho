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

package com.facebook.samples.litho.kotlin.documentation

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.core.margin
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.SolidColor
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign

class SimpleAllLayoutTransitionKComponent : KComponent() {

  // layout_start
  override fun ComponentScope.render(): Component {
    val toRight = useState { false }
    return Column(
        style = Style.margin(all = 10.dp).onClick { toRight.update { !it } },
        alignItems = if (toRight.value) YogaAlign.FLEX_END else YogaAlign.FLEX_START) {
          child(SolidColor.create(context).color(Color.YELLOW).widthDip(80f).heightDip(80f).build())
        }
  }
  // layout_end
}

class SimpleAllLayoutTransitionKComponentV2 : KComponent() {

  override fun ComponentScope.render(): Component {
    val toRight = useState { false }
    // transition_start
    useTransition(Transition.allLayout())
    // transition_end
    return Column(
        style = Style.margin(all = 10.dp).onClick { toRight.update { !it } },
        alignItems = if (toRight.value) YogaAlign.FLEX_END else YogaAlign.FLEX_START) {
          child(SolidColor.create(context).color(Color.YELLOW).widthDip(80f).heightDip(80f).build())
        }
  }
}
