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

package com.fblitho.lithoktsample.animations.animationcomposition

import android.graphics.Color
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State
import com.facebook.yoga.YogaAlign

@LayoutSpec
object OneByOneLeftRightBlocksComponentSpec {

  private const val TRANSITION_KEY_RED = "red"
  private const val TRANSITION_KEY_BLUE = "blue"
  private const val TRANSITION_KEY_GREEN = "green"
  private val ALL_TRANSITION_KEYS = arrayOf(
      TRANSITION_KEY_RED,
      TRANSITION_KEY_BLUE,
      TRANSITION_KEY_GREEN
  )

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @State state: Int): Component {
    val redLeft = state == 0 || state == 4 || state == 5
    val blueLeft = state == 0 || state == 1 || state == 5
    val greenLeft = state == 0 || state == 1 || state == 2

    return Column.create(c)
        .child(
            Column.create(c)
                .alignItems(if (redLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40f)
                        .widthDip(40f)
                        .backgroundColor(Color.parseColor("#ee1111"))
                        .transitionKey(TRANSITION_KEY_RED)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .build()))
        .child(
            Column.create(c)
                .alignItems(if (blueLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40f)
                        .widthDip(40f)
                        .backgroundColor(Color.parseColor("#1111ee"))
                        .transitionKey(TRANSITION_KEY_BLUE)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .build()))
        .child(
            Column.create(c)
                .alignItems(if (greenLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40f)
                        .widthDip(40f)
                        .backgroundColor(Color.parseColor("#11ee11"))
                        .transitionKey(TRANSITION_KEY_GREEN)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .build()))
        .clickHandler(OneByOneLeftRightBlocksComponent.onClick(c))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext) {
    OneByOneLeftRightBlocksComponent.updateState(c)
  }

  @OnUpdateState
  fun updateState(state: StateValue<Int>) {
    state.set((state.get()!! + 1) % 6)
  }

  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext): Transition =
      Transition.create(Transition.TransitionKeyType.GLOBAL, *ALL_TRANSITION_KEYS)
          .animate(AnimatedProperties.X, AnimatedProperties.Y, AnimatedProperties.ALPHA)
}
