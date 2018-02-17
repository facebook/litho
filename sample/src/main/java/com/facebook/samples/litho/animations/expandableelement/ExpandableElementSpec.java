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
package com.facebook.samples.litho.animations.expandableelement;

import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.support.annotation.Nullable;
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
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class ExpandableElementSpec {

  private static final String TRANSITION_MSG_PARENT = "transition_msg_parent";
  private static final String TRANSITION_TEXT_MESSAGE_WITH_BOTTOM =
      "transition_text_msg_with_bottom";
  private static final String TRANSITION_TOP_DETAIL = "transition_top_detail";
  private static final String TRANSITION_BOTTOM_DETAIL = "transition_bottom_detail";

  private static final String MESSAGE_BODY =
      "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque "
          + "laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi "
          + "architecto beatae vitae dicta sunt explicabo.";

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean expanded) {
    return Column.create(c)
        .backgroundColor(0xfff0f0f0)
        .paddingDip(YogaEdge.TOP, 8)
        .transitionKey(TRANSITION_MSG_PARENT)
        .clickHandler(ExpandableElement.onClick(c))
        .child(maybeCreateTopDetailComponent(c, expanded))
        .child(
            Column.create(c)
                .transitionKey(TRANSITION_TEXT_MESSAGE_WITH_BOTTOM)
                .child(Row.create(c).child(createSenderTile(c)).child(createMessageContent(c)))
                .child(maybeCreateBottomDetailComponent(c, expanded)))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @State boolean expanded) {
    ExpandableElement.updateExpandedState(c, !expanded);
  }

  @OnUpdateState
  static void updateExpandedState(StateValue<Boolean> expanded, @Param boolean expand) {
    expanded.set(expand);
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.parallel(
        Transition.create(TRANSITION_TOP_DETAIL)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0)
            .disappearTo(0),
        Transition.create(TRANSITION_TEXT_MESSAGE_WITH_BOTTOM).animate(AnimatedProperties.Y),
        Transition.create(TRANSITION_MSG_PARENT).animate(AnimatedProperties.HEIGHT).appearFrom(0),
        Transition.create(TRANSITION_BOTTOM_DETAIL)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0)
            .disappearTo(0));
  }

  @Nullable
  static Component.Builder maybeCreateTopDetailComponent(ComponentContext c, boolean expanded) {
    if (!expanded) {
      return null;
    }

    return Text.create(c)
        .textSizeDip(14)
        .textColor(Color.GRAY)
        .alignSelf(YogaAlign.CENTER)
        .transitionKey(TRANSITION_TOP_DETAIL)
        .clipToBounds(true)
        .text("DEC 25 AT 9:55");
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

  static ShapeDrawable getMessageBackground(ComponentContext c) {
    final RoundRectShape roundedRectShape =
        new RoundRectShape(
            new float[] {40, 40, 40, 40, 40, 40, 40, 40},
            null,
            new float[] {40, 40, 40, 40, 40, 40, 40, 40});
    final ShapeDrawable oval = new ShapeDrawable(roundedRectShape);
    oval.getPaint().setColor(0xff2b6dd8);
    return oval;
  }

  static Component.Builder createMessageContent(ComponentContext c) {
    return Row.create(c)
        .paddingDip(YogaEdge.ALL, 8)
        .marginDip(YogaEdge.ALL, 8)
        .background(getMessageBackground(c))
        .child(Text.create(c).textSizeDip(18).textColor(Color.WHITE).text(MESSAGE_BODY));
  }

  @Nullable
  static Component.Builder maybeCreateBottomDetailComponent(ComponentContext c, boolean expanded) {
    if (!expanded) {
      return null;
    }

    return Text.create(c)
        .textSizeDip(14)
        .textColor(Color.GRAY)
        .alignSelf(YogaAlign.FLEX_END)
        .paddingDip(YogaEdge.RIGHT, 10)
        .clipToBounds(true)
        .transitionKey(TRANSITION_BOTTOM_DETAIL)
        .text("Seen");
  }
}
