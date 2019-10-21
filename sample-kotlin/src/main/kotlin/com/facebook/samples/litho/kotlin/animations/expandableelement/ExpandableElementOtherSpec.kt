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

package com.facebook.samples.litho.kotlin.animations.expandableelement

import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.Transition.BaseTransitionUnitsBuilder
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateStateWithTransition
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge

@LayoutSpec
object ExpandableElementOtherSpec {

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop messageText: String,
      @Prop timestamp: String,
      @Prop(optional = true) seen: Boolean,
      @State expanded: Boolean?
  ): Component {
    val isExpanded = expanded ?: false

    return Column.create(c)
        .paddingDip(YogaEdge.TOP, 8f)
        .transitionKey(ExpandableElementUtil.TRANSITION_MSG_PARENT)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .clickHandler(ExpandableElementOther.onClick(c))
        .child(ExpandableElementUtil.maybeCreateTopDetailComponent(c, isExpanded, timestamp))
        .child(
            Column.create(c)
                .transitionKey(ExpandableElementUtil.TRANSITION_TEXT_MESSAGE_WITH_BOTTOM)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .child(
                    Row.create(c)
                        .paddingDip(YogaEdge.END, 5f)
                        .child(createSenderTile(c))
                        .child(createMessageContent(c, messageText)))
                .child(ExpandableElementUtil.maybeCreateBottomDetailComponent(c, isExpanded, seen)))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext, @State expanded: Boolean?) {
    val isExpanded = expanded ?: false
    ExpandableElementOther.updateExpandedStateWithTransition(c, !isExpanded)
  }

  @OnUpdateStateWithTransition
  fun updateExpandedState(expanded: StateValue<Boolean>, @Param expand: Boolean): Transition {
    expanded.set(expand)

    return Transition.parallel<BaseTransitionUnitsBuilder>(
        Transition.allLayout(),
        Transition.create(Transition.TransitionKeyType.GLOBAL, ExpandableElementUtil.TRANSITION_TOP_DETAIL)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0f)
            .disappearTo(0f),
        Transition.create(Transition.TransitionKeyType.GLOBAL, ExpandableElementUtil.TRANSITION_BOTTOM_DETAIL)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0f)
            .disappearTo(0f))
  }

  private fun createSenderTile(c: ComponentContext): Component.Builder<*> =
      Row.create(c)
          .marginDip(YogaEdge.ALL, 5f)
          .alignSelf(YogaAlign.CENTER)
          .widthDip(55f)
          .heightDip(55f)
          .flexShrink(0f)
          .background(getCircle(c))

  private fun getCircle(c: ComponentContext): ShapeDrawable = ShapeDrawable(OvalShape())
      .also {
        it.paint.color = Color.LTGRAY
      }

  private fun createMessageContent(c: ComponentContext, messageText: String): Component.Builder<*> =
      Row.create(c)
          .paddingDip(YogaEdge.ALL, 8f)
          .marginDip(YogaEdge.ALL, 8f)
          .background(ExpandableElementUtil.getMessageBackground(c, 0xFFEAEAEA.toInt()))
          .child(Text.create(c).textSizeDip(18f).textColor(Color.BLACK).text(messageText))
}
