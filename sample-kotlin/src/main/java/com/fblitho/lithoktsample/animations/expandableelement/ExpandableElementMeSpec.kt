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

package com.fblitho.lithoktsample.animations.expandableelement

import android.graphics.Color
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
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import com.fblitho.lithoktsample.animations.expandableelement.ExpandableElementUtil.TRANSITION_MSG_PARENT

@LayoutSpec
object ExpandableElementMeSpec {

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
                        .paddingDip(YogaEdge.START, 75f)
                        .paddingDip(YogaEdge.END, 5f)
                        .child(createMessageContent(c, messageText)))
                .child(ExpandableElementUtil.maybeCreateBottomDetailComponent(c, isExpanded, seen)))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext, @State expanded: Boolean?) {
    val isExpanded = expanded ?: false
    ExpandableElementMe.updateExpandedState(c, !isExpanded)
  }

  @OnUpdateState
  fun updateExpandedState(expanded: StateValue<Boolean>, @Param expand: Boolean) {
    expanded.set(expand)
  }

  @OnCreateTransition
  fun onCreateTransition(
      c: ComponentContext,
      @State expanded: Boolean?,
      @Prop(optional = true) forceAnimateOnAppear: Boolean
  ): Transition? = if (!forceAnimateOnAppear && expanded == null) {
    null
  } else {
    Transition.parallel<BaseTransitionUnitsBuilder>(
        Transition.allLayout(),
        Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_MSG_PARENT).animate(AnimatedProperties.HEIGHT).appearFrom(0f),
        Transition.create(Transition.TransitionKeyType.GLOBAL, ExpandableElementUtil.TRANSITION_TOP_DETAIL)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0f)
            .disappearTo(0f),
        Transition.create(Transition.TransitionKeyType.GLOBAL, ExpandableElementUtil.TRANSITION_BOTTOM_DETAIL)
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(0f)
            .disappearTo(0f))
  }

  private fun createMessageContent(c: ComponentContext, messageText: String): Component.Builder<*> =
      Row.create(c)
          .paddingDip(YogaEdge.ALL, 8f)
          .marginDip(YogaEdge.ALL, 8f)
          .background(ExpandableElementUtil.getMessageBackground(c, 0xFF0084FF.toInt()))
          .child(Text.create(c).textSizeDip(18f).textColor(Color.WHITE).text(messageText))
}
