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

package com.facebook.samples.litho.java.animations.animationcomposition;

import android.graphics.Color;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Diff;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.DimensionValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;

@LayoutSpec
public class StoryFooterComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean commentText) {
    return !commentText
        ? Row.create(c)
            .backgroundColor(Color.WHITE)
            .heightDip(56)
            .child(
                Row.create(c)
                    .widthPercent(33.3f)
                    .alignItems(YogaAlign.CENTER)
                    .justifyContent(YogaJustify.CENTER)
                    .clickHandler(StoryFooterComponent.onClick(c))
                    .testKey("like_button")
                    .child(
                        Column.create(c)
                            .heightDip(24)
                            .widthDip(24)
                            .backgroundColor(Color.RED)
                            .transitionKey("icon_like"))
                    .child(
                        Text.create(c)
                            .textSizeSp(16)
                            .text("Like")
                            .transitionKey("text_like")
                            .marginDip(YogaEdge.LEFT, 8)))
            .child(
                Row.create(c)
                    .transitionKey("cont_comment")
                    .widthPercent(33.3f)
                    .alignItems(YogaAlign.CENTER)
                    .justifyContent(YogaJustify.CENTER)
                    .child(Column.create(c).heightDip(24).widthDip(24).backgroundColor(Color.RED))
                    .child(
                        Text.create(c).textSizeSp(16).text("Comment").marginDip(YogaEdge.LEFT, 8)))
            .child(
                Row.create(c)
                    .widthPercent(33.3f)
                    .alignItems(YogaAlign.CENTER)
                    .justifyContent(YogaJustify.CENTER)
                    .child(
                        Column.create(c)
                            .transitionKey("icon_share")
                            .heightDip(24)
                            .widthDip(24)
                            .backgroundColor(Color.RED))
                    .child(
                        Text.create(c)
                            .textSizeSp(16)
                            .text("Share")
                            .transitionKey("text_share")
                            .marginDip(YogaEdge.LEFT, 8)))
            .build()
        : Row.create(c)
            .backgroundColor(Color.WHITE)
            .heightDip(56)
            .child(
                Row.create(c)
                    .alignItems(YogaAlign.CENTER)
                    .justifyContent(YogaJustify.CENTER)
                    .clickHandler(StoryFooterComponent.onClick(c))
                    .paddingDip(YogaEdge.HORIZONTAL, 16)
                    .testKey("like_button")
                    .child(
                        Column.create(c)
                            .transitionKey("icon_like")
                            .heightDip(24)
                            .widthDip(24)
                            .backgroundColor(Color.RED)))
            .child(
                Column.create(c)
                    .flexGrow(1)
                    .transitionKey("comment_editText")
                    .child(Text.create(c).text("Input here").textSizeSp(16)))
            .child(
                Row.create(c)
                    .transitionKey("cont_share")
                    .alignItems(YogaAlign.CENTER)
                    .clickHandler(StoryFooterComponent.onClick(c))
                    .paddingDip(YogaEdge.ALL, 16)
                    .backgroundColor(0xff0000ff)
                    .child(
                        Column.create(c)
                            .transitionKey("icon_share")
                            .heightDip(24)
                            .widthDip(24)
                            .backgroundColor(Color.RED)))
            .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    StoryFooterComponent.updateStateSync(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> commentText) {
    commentText.set(commentText.get() == true ? false : true);
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c, @State Diff<Boolean> commentText) {
    if (commentText.getPrevious() == null) {
      return null;
    }

    return Transition.parallel(
        Transition.create("comment_editText")
            .animate(AnimatedProperties.ALPHA)
            .appearFrom(0)
            .disappearTo(0)
            .animate(AnimatedProperties.X)
            .appearFrom(DimensionValue.widthPercentageOffset(-50))
            .disappearTo(DimensionValue.widthPercentageOffset(-50)),
        Transition.create("cont_comment")
            .animate(AnimatedProperties.ALPHA)
            .appearFrom(0)
            .disappearTo(0),
        Transition.create("icon_like", "icon_share").animate(AnimatedProperties.X),
        Transition.create("text_like", "text_share")
            .animate(AnimatedProperties.ALPHA)
            .appearFrom(0)
            .disappearTo(0)
            .animate(AnimatedProperties.X)
            .appearFrom(DimensionValue.widthPercentageOffset(50))
            .disappearTo(DimensionValue.widthPercentageOffset(50)));
  }
}
