/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.facebook.samples.litho.transitionsdemo;

import android.graphics.Color;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
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
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @State boolean commentText) {
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
                        .withLayout()
                        .transitionKey("text_like")
                        .marginDip(YogaEdge.LEFT, 8)))
        .child(
            Row.create(c)
                .transitionKey("cont_comment")
                .widthPercent(33.3f)
                .alignItems(YogaAlign.CENTER)
                .justifyContent(YogaJustify.CENTER)
                .child(
                    Column.create(c)
                        .heightDip(24)
                        .widthDip(24)
                        .backgroundColor(Color.RED))
                .child(
                    Text.create(c)
                        .textSizeSp(16)
                        .text("Comment")
                        .withLayout()
                        .marginDip(YogaEdge.LEFT, 8)))
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
                        .withLayout()
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
                .child(
                    Text.create(c)
                        .text("Input here")
                        .textSizeSp(16)))
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
    StoryFooterComponent.updateState(c);
  }

  @OnUpdateState
  static void updateState(
      StateValue<Boolean> commentText) {
    commentText.set(commentText.get() == true ? false : true);
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.createSet(
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
        Transition.create("icon_like", "icon_share")
            .animate(AnimatedProperties.X),
        Transition.create("text_like", "text_share")
            .animate(AnimatedProperties.ALPHA)
            .appearFrom(0)
            .disappearTo(0)
            .animate(AnimatedProperties.X)
            .appearFrom(DimensionValue.widthPercentageOffset(50))
            .disappearTo(DimensionValue.widthPercentageOffset(50)));
  }
}
