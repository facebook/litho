/*
* Copyright 2019-present Facebook, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.facebook.litho.codelab.auxiliary

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.ResType
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge

@Suppress("MagicNumber")
@LayoutSpec
object TimelineComponentSpec {
  private const val TRANSITION_KEY = "transitionKey"

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(resType = ResType.STRING) type: String,
      @Prop(resType = ResType.COLOR) color: Int,
      @Prop timestamp: Long
  ): Component {
    return Column.create(c)
        .transitionKey(TRANSITION_KEY)
        .child(Text.create(c).text(type).textSizeSp(10f))
        .child(Text.create(c).text("${timestamp}ms").textSizeSp(10f))
        .alignContent(YogaAlign.CENTER)
        .heightDip(50f)
        .paddingDip(YogaEdge.ALL, 8f)
        .backgroundColor(color)
        .build()
  }

  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext): Transition {
    return Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY)
        .animate(AnimatedProperties.ALPHA)
        .appearFrom(0f)
        .animator(Transition.timing(800))
  }
}
