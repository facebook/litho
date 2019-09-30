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

import static com.facebook.samples.litho.animations.expandableelement.ExpandableElementUtil.TRANSITION_MSG_PARENT;

import android.graphics.Color;
import androidx.annotation.Nullable;
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
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;

@LayoutSpec
public class ExpandableElementMeSpec {

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
        .transitionKey(TRANSITION_MSG_PARENT)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .clickHandler(ExpandableElementMe.onClick(c))
        .child(ExpandableElementUtil.maybeCreateTopDetailComponent(c, isExpanded, timestamp))
        .child(
            Column.create(c)
                .transitionKey(ExpandableElementUtil.TRANSITION_TEXT_MESSAGE_WITH_BOTTOM)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .child(
                    Row.create(c)
                        .justifyContent(YogaJustify.FLEX_END)
                        .paddingDip(YogaEdge.START, 75)
                        .paddingDip(YogaEdge.END, 5)
                        .child(createMessageContent(c, messageText)))
                .child(ExpandableElementUtil.maybeCreateBottomDetailComponent(c, isExpanded, seen)))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @State Boolean expanded) {
    final boolean isExpanded = expanded == null ? false : expanded;
    ExpandableElementMe.updateExpandedStateSync(c, !isExpanded);
  }

  @OnUpdateState
  static void updateExpandedState(StateValue<Boolean> expanded, @Param boolean expand) {
    expanded.set(expand);
  }

  @Nullable
  @OnCreateTransition
  static Transition onCreateTransition(
      ComponentContext c,
      @State Boolean expanded,
      @Prop(optional = true) boolean forceAnimateOnAppear) {
    if (!forceAnimateOnAppear && expanded == null) {
      return null;
    }

    return Transition.parallel(
        Transition.allLayout(),
        Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_MSG_PARENT)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0),
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

  static Component.Builder createMessageContent(ComponentContext c, String messageText) {
    return Row.create(c)
        .paddingDip(YogaEdge.ALL, 8)
        .marginDip(YogaEdge.ALL, 8)
        .background(ExpandableElementUtil.getMessageBackground(c, 0xFF0084FF))
        .child(Text.create(c).textSizeDip(18).textColor(Color.WHITE).text(messageText));
  }
}
