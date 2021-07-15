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

package com.facebook.samples.litho.java.animations.expandableelement;

import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class ExpandableElementOtherSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop String messageText,
      @Prop String timestamp,
      @Prop(optional = true) boolean seen,
      @State Boolean expanded) {
    final boolean isExpanded = expanded == null ? false : expanded;
    return Column.create(c)
        .paddingDip(YogaEdge.TOP, 8)
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
                        .paddingDip(YogaEdge.END, 5)
                        .child(createSenderTile(c))
                        .child(createMessageContent(c, messageText)))
                .child(ExpandableElementUtil.maybeCreateBottomDetailComponent(c, isExpanded, seen)))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @State Boolean expanded) {
    final boolean isExpanded = expanded == null ? false : expanded;
    ExpandableElementOther.updateExpandedStateWithTransition(c, !isExpanded);
  }

  @OnUpdateStateWithTransition
  static Transition updateExpandedState(StateValue<Boolean> expanded, @Param boolean expand) {
    expanded.set(expand);

    return Transition.parallel(
        Transition.allLayout(),
        Transition.create(
                Transition.TransitionKeyType.GLOBAL, ExpandableElementUtil.TRANSITION_TOP_DETAIL)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0)
            .disappearTo(0),
        Transition.create(
                Transition.TransitionKeyType.GLOBAL, ExpandableElementUtil.TRANSITION_BOTTOM_DETAIL)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0)
            .disappearTo(0));
  }

  static Component.Builder createSenderTile(ComponentContext c) {
    return Row.create(c)
        .marginDip(YogaEdge.ALL, 5)
        .alignSelf(YogaAlign.CENTER)
        .widthDip(55)
        .heightDip(55)
        .flexShrink(0)
        .background(getCircle(c));
  }

  static ShapeDrawable getCircle(ComponentContext c) {
    final ShapeDrawable oval = new ShapeDrawable(new OvalShape());
    oval.getPaint().setColor(Color.LTGRAY);
    return oval;
  }

  static Component.Builder createMessageContent(ComponentContext c, String messageText) {
    return Row.create(c)
        .paddingDip(YogaEdge.ALL, 8)
        .marginDip(YogaEdge.ALL, 8)
        .background(ExpandableElementUtil.getMessageBackground(c, 0xFFEAEAEA))
        .child(Text.create(c).textSizeDip(18).textColor(Color.BLACK).text(messageText));
  }
}
