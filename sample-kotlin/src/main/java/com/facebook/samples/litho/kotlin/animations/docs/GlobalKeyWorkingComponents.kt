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

package com.facebook.samples.litho.kotlin.animations.docs

import android.graphics.Color
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign

private const val SQUARE_KEY = "square"

// start_working_parent
// Parent component that defines the transition for the GLOBAL key
class GlobalKeyParentComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    // This transition WILL work because we specify GLOBAL key type
    useTransition(
        Transition.create(Transition.TransitionKeyType.GLOBAL, SQUARE_KEY)
            .animate(AnimatedProperties.X),
    )

    return GlobalKeyTransitionComponent()
  }
}

// end_working_parent

// start_working_child
// Child component that assigns the GLOBAL transition key
class GlobalKeyTransitionComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val toRight = useState { false }

    return Column(
        alignItems = if (toRight.value) YogaAlign.FLEX_END else YogaAlign.FLEX_START,
        style = Style.onClick { toRight.update { !it } },
    ) {
      child(
          Row(
              style =
                  Style.width(80.dp)
                      .height(80.dp)
                      .backgroundColor(Color.YELLOW)
                      // Using GLOBAL key - visible throughout tree
                      .transitionKey(context, SQUARE_KEY, Transition.TransitionKeyType.GLOBAL),
          ),
      )
    }
  }
}
// end_working_child
