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

package com.facebook.samples.litho.kotlin.animations.animationcomposition

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.animation.DimensionValue
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sp
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.view.testKey
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

class StoryFooterComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val commentText = useState { false }
    useTransition(
        Transition.parallel(
            Transition.create(Transition.TransitionKeyType.GLOBAL, "comment_editText")
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)
                .animate(AnimatedProperties.X)
                .appearFrom(DimensionValue.widthPercentageOffset(-50f))
                .disappearTo(DimensionValue.widthPercentageOffset(-50f)),
            Transition.create(Transition.TransitionKeyType.GLOBAL, "cont_comment")
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f),
            Transition.create(Transition.TransitionKeyType.GLOBAL, "icon_like", "icon_share")
                .animate(AnimatedProperties.X),
            Transition.create(Transition.TransitionKeyType.GLOBAL, "text_like", "text_share")
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)
                .animate(AnimatedProperties.X)
                .appearFrom(DimensionValue.widthPercentageOffset(50f))
                .disappearTo(DimensionValue.widthPercentageOffset(50f))))
    if (!commentText.value) {
      return Row(style = Style.backgroundColor(Color.WHITE).height(56f.dp)) {
        child(
            Row(
                alignItems = YogaAlign.CENTER,
                justifyContent = YogaJustify.CENTER,
                style =
                    Style.widthPercent(33.3f)
                        .onClick { commentText.update(!commentText.value) }
                        .testKey("like_button")
                        .transitionKey(context, "icon_like", Transition.TransitionKeyType.GLOBAL)) {
              child(
                  Column(
                      style =
                          Style.height(24f.dp)
                              .width(24f.dp)
                              .backgroundColor(Color.RED)
                              .transitionKey(
                                  context, "icon_like", Transition.TransitionKeyType.GLOBAL)))
              child(
                  Text(
                      textSize = 16f.sp,
                      text = "Like",
                      style =
                          Style.transitionKey(
                                  context, "text_like", Transition.TransitionKeyType.GLOBAL)
                              .margin(left = 8f.dp)))
            })
        child(
            Row(
                alignItems = YogaAlign.CENTER,
                justifyContent = YogaJustify.CENTER,
                style =
                    Style.transitionKey(
                            context, "cont_comment", Transition.TransitionKeyType.GLOBAL)
                        .widthPercent(33.3f)) {
              child(Column(style = Style.height(24f.dp).width(24f.dp).backgroundColor(Color.RED)))
              child(Text(textSize = 16f.sp, text = "Comment", style = Style.margin(left = 8f.dp)))
            })
        child(
            Row(
                alignItems = YogaAlign.CENTER,
                justifyContent = YogaJustify.CENTER,
                style = Style.widthPercent(33.3f)) {
              child(
                  Column(
                      style =
                          Style.transitionKey(
                                  context, "icon_share", Transition.TransitionKeyType.GLOBAL)
                              .height(24f.dp)
                              .width(24f.dp)
                              .backgroundColor(Color.RED)))
              child(
                  Text(
                      textSize = 16f.sp,
                      text = "Share",
                      style =
                          Style.transitionKey(
                                  context, "text_share", Transition.TransitionKeyType.GLOBAL)
                              .margin(left = 8f.dp)))
            })
      }
    } else {
      return Row(style = Style.backgroundColor(Color.WHITE).height(56f.dp)) {
        child(
            Row(
                alignItems = YogaAlign.CENTER,
                justifyContent = YogaJustify.CENTER,
                style =
                    Style.onClick { commentText.update(!commentText.value) }
                        .padding(horizontal = 16f.dp)
                        .testKey("like_button")) {
              child(
                  Column(
                      style =
                          Style.transitionKey(
                                  context, "icon_like", Transition.TransitionKeyType.GLOBAL)
                              .height(24f.dp)
                              .width(24f.dp)
                              .backgroundColor(Color.RED)))
            })
        child(
            Column(
                style =
                    Style.flex(grow = 1f)
                        .transitionKey(
                            context, "comment_editText", Transition.TransitionKeyType.GLOBAL)) {
              child(Text(text = "Input here", textSize = 16f.sp))
            })
        child(
            Row(
                alignItems = YogaAlign.CENTER,
                style =
                    Style.transitionKey(context, "cont_share", Transition.TransitionKeyType.GLOBAL)
                        .onClick { commentText.update(!commentText.value) }
                        .padding(all = 16f.dp)
                        .backgroundColor(0xFF0000FF.toInt())) {
              child(
                  Column(
                      style =
                          Style.transitionKey(
                                  context, "icon_share", Transition.TransitionKeyType.GLOBAL)
                              .height(24f.dp)
                              .width(24f.dp)
                              .backgroundColor(Color.RED)))
            })
      }
    }
  }
}
